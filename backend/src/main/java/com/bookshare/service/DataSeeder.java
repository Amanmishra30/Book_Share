package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Creates the community tree, the admin account and (optionally) two demo readers with a few books. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CommunityRepository communities;
    private final UserRepository users;
    private final BookRepository books;
    private final RoomRepository rooms;
    private final ReadingRoomRepository readingRooms;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email}") private String adminEmail;
    @Value("${app.admin.password}") private String adminPassword;
    @Value("${app.seed-demo}") private boolean seedDemo;

    private Community community(String name, CommunityLevel level, Community parent) {
        Community c = new Community();
        c.setName(name);
        c.setLevel(level);
        c.setParent(parent);
        return communities.save(c);
    }

    private User user(String name, String email, String password, Community c, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setCommunity(c);
        u.setRole(role);
        return users.save(u);
    }

    private Book book(User owner, String title, String author, String genre, BookCondition cond, ListingType type, String price, int days, String desc) {
        Book b = new Book();
        b.setOwner(owner);
        b.setCommunity(owner.getCommunity());
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setBookCondition(cond);
        b.setListingType(type);
        b.setPrice(price == null ? null : new BigDecimal(price));
        b.setLendingDurationDays(days);
        b.setDescription(desc);
        return books.save(b);
    }

    @Override
    @Transactional
    public void run(String... args) {
        Community city;
        Community hollow;
        if (communities.count() == 0) {
            city = community("Evergreen City", CommunityLevel.CITY, null);
            Community mossgate = community("Mossgate", CommunityLevel.NEIGHBOURHOOD, city);
            hollow = community("Hollow Hall Hostel", CommunityLevel.BUILDING, mossgate);
            community("Lantern Lane Apartments", CommunityLevel.BUILDING, mossgate);
            log.info("Seeded the community tree (Evergreen City > Mossgate > two buildings).");
        } else {
            city = communities.findAll().stream().filter(c -> c.getParent() == null).findFirst().orElseThrow();
            hollow = city;
        }
        if (!users.existsByEmailIgnoreCase(adminEmail)) {
            user("BookShare Admin", adminEmail, adminPassword, city, Role.ADMIN);
            log.info("Created admin account {}", adminEmail);
        }
        if (!seedDemo || users.existsByEmailIgnoreCase("asha@demo.local")) return;

        Community hall = communities.findAll().stream().filter(c -> c.getName().equals("Hollow Hall Hostel")).findFirst().orElse(hollow);
        Community lane = communities.findAll().stream().filter(c -> c.getName().equals("Lantern Lane Apartments")).findFirst().orElse(hall);
        User asha = user("Asha Reader", "asha@demo.local", "Demo@1234", hall, Role.USER);
        User rohan = user("Rohan Page", "rohan@demo.local", "Demo@1234", hall, Role.USER);
        User meera = user("Meera Verse", "meera@demo.local", "Demo@1234", lane, Role.USER);

        book(asha, "The Hobbit", "J.R.R. Tolkien", "Fantasy", BookCondition.GOOD, ListingType.BORROW, null, 14, "A cosy paperback, a little worn at the corners.");
        book(asha, "Pride and Prejudice", "Jane Austen", "Classic", BookCondition.LIKE_NEW, ListingType.BOTH, "150", 21, "Hardcover with a ribbon bookmark.");
        book(rohan, "Wings of Fire", "A.P.J. Abdul Kalam", "Biography", BookCondition.GOOD, ListingType.BORROW, null, 14, "Underlined in pencil in a few places.");
        book(rohan, "The Alchemist", "Paulo Coelho", "Fiction", BookCondition.FAIR, ListingType.SALE, "100", 14, "Well loved, spine is soft.");
        book(meera, "Gitanjali", "Rabindranath Tagore", "Poetry", BookCondition.GOOD, ListingType.BORROW, null, 14, "Pocket edition from Lantern Lane.");

        Room room = new Room();
        room.setCreatedBy(asha);
        room.setName("The Cosy Corner");
        room.setTopic("General book chat");
        rooms.save(room);

        Room club = new Room();
        club.setCreatedBy(rohan);
        club.setName("Monsoon Book Club");
        club.setTopic("Classics");
        club.setType(RoomType.BOOK_CLUB);
        club.setScheduledAt(Instant.now().plus(7, ChronoUnit.DAYS));
        rooms.save(club);

        ReadingRoom rr = new ReadingRoom();
        rr.setHost(asha);
        rr.setTitle("Quiet Reading Hour");
        rr.setDescription("Bring a book, bring tea. We read together in silence.");
        rr.setScheduledTime(Instant.now().plus(2, ChronoUnit.DAYS));
        rr.setMaxParticipants(6);
        rr.getParticipants().add(asha);
        readingRooms.save(rr);
        log.info("Seeded demo users asha/rohan/meera@demo.local (password Demo@1234).");
    }
}
