package com.princekumar.itams.user;

import com.princekumar.itams.person.Person;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserDisplayNamesTest {

    private final UserAccountRepository repo = mock(UserAccountRepository.class);
    private final UserDisplayNames names = new UserDisplayNames(repo);

    @Test
    void maps_ids_to_full_names_with_one_query_ignoring_nulls_and_duplicates() {
        // Built first: Mockito does not allow stubbing a mock inside another when(...).
        var lena = account(4L, "Lena", "Wagner");
        var jonas = account(7L, "Jonas", "Becker");
        when(repo.findByIdIn(List.of(4L, 7L))).thenReturn(List.of(lena, jonas));

        var result = names.of(Arrays.asList(4L, null, 7L, 4L));

        assertThat(result).containsEntry(4L, "Lena Wagner").containsEntry(7L, "Jonas Becker").hasSize(2);
        verify(repo, times(1)).findByIdIn(any());
    }

    @Test
    void no_ids_means_no_query_and_lookups_of_null_are_safe() {
        var result = names.of(Arrays.asList((Long) null));

        assertThat(result.get(null)).isNull();
        verifyNoInteractions(repo);
    }

    private static UserAccount account(long id, String first, String last) {
        UserAccount u = mock(UserAccount.class);
        when(u.getId()).thenReturn(id);
        when(u.getPerson()).thenReturn(new Person(first, last, first.toLowerCase() + "@example.com", null));
        return u;
    }
}
