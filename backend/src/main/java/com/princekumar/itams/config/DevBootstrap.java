package com.princekumar.itams.config;

import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import com.princekumar.itams.user.Role;
import com.princekumar.itams.user.RoleRepository;
import com.princekumar.itams.user.UserAccount;
import com.princekumar.itams.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps three demo users on first startup in <b>non-prod profiles</b> —
 * one per role — so you can log in immediately without a manual setup step.
 *
 * <p>The demo password comes from {@code BOOTSTRAP_DEMO_PASSWORD} in the
 * environment (default {@code "changeme"}). Change it before showing the
 * project to anyone else.</p>
 *
 * <p>Idempotent: skips any user that already exists.</p>
 */
@Configuration
@Profile("!prod")
public class DevBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DevBootstrap.class);

    @Bean
    CommandLineRunner seedDemoUsers(
        PersonRepository personRepo,
        UserAccountRepository userRepo,
        RoleRepository roleRepo,
        PasswordEncoder encoder,
        @Value("${bootstrap.demo-password:changeme}") String demoPassword
    ) {
        return args -> {
            create(personRepo, userRepo, roleRepo, encoder, demoPassword,
                "admin",     "Admin",   "User",     "admin@itams.local",     "ADMIN");
            create(personRepo, userRepo, roleRepo, encoder, demoPassword,
                "itmanager", "IT",      "Manager",  "itmanager@itams.local", "IT_MANAGER");
            create(personRepo, userRepo, roleRepo, encoder, demoPassword,
                "employee",  "Regular", "Employee", "employee@itams.local",  "EMPLOYEE");
        };
    }

    @Transactional
    void create(PersonRepository personRepo, UserAccountRepository userRepo,
                RoleRepository roleRepo, PasswordEncoder encoder, String demoPassword,
                String username, String firstName, String lastName,
                String email, String roleCode) {
        if (userRepo.existsByUsername(username)) {
            return;
        }
        Person person = personRepo.save(new Person(firstName, lastName, email, null));
        UserAccount user = new UserAccount(person, username, encoder.encode(demoPassword));
        Role role = roleRepo.findByCode(roleCode)
            .orElseThrow(() -> new IllegalStateException("Role '" + roleCode + "' missing — did V2 seed run?"));
        user.addRole(role);
        userRepo.save(user);
        log.info("[DevBootstrap] Seeded {} (role: {})", username, roleCode);
    }
}
