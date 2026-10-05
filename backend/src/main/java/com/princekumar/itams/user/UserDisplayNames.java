package com.princekumar.itams.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Turns user-account ids (ticket assignee, comment author, maintenance
 * performer) into "First Last" for API responses. Those tables only store the
 * user id, so without this the UI could only show "User #4".
 */
@Service
public class UserDisplayNames {

    private final UserAccountRepository repo;

    public UserDisplayNames(UserAccountRepository repo) { this.repo = repo; }

    /** One query for a whole page of rows. Ids that don't exist are simply missing from the map. */
    @Transactional(readOnly = true)
    public Map<Long, String> of(Collection<Long> userIds) {
        var ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();   // unlike Map.of(), get(null) returns null here
        }
        return repo.findByIdIn(ids).stream().collect(Collectors.toMap(
            UserAccount::getId,
            u -> u.getPerson().getFirstName() + " " + u.getPerson().getLastName()));
    }
}
