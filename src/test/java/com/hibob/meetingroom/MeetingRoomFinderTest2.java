package com.hibob.meetingroom;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeetingRoomFinderTest2 {

    private MeetingRoom roomA; // Capacity 10, Room A
    private MeetingRoom roomB; // Capacity 20, Room B
    private MeetingRoom roomC; // Capacity 30, Room C
    private MeetingRoomFinder meetingRoomFinder;

    @BeforeEach
    void setUp() {
        roomA = new MeetingRoom(10, "Room A");
        roomB = new MeetingRoom(20, "Room B");
        roomC = new MeetingRoom(30, "Room C");

        // Use List.of() for immutable test data where order matters for the constructor
        List<MeetingRoom> rooms = List.of(roomA, roomC, roomB); // Order doesn't matter for initial set, as it's sorted
        meetingRoomFinder = new MeetingRoomFinder(rooms);
    }

    @Test
    @DisplayName("Should successfully book an available meeting room")
    void testBookMeetingRoomSuccessfully() {
        LocalDate date = LocalDate.of(2024, 3, 20);
        Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, 15); // Room B (capacity 20) is smallest >= 15

        assertTrue(bookedRoom.isPresent(), "A room should be booked successfully");
        assertEquals("Room B", bookedRoom.get().getRoomName(), "The correct room (Room B) should be booked");
        assertEquals(20, bookedRoom.get().getCapacity(), "The booked room's capacity should be 20");
    }

    @Test
    @DisplayName("Should correctly handle booking for unavailable rooms")
    void testBookMeetingRoomNotAvailable() {
        LocalDate date = LocalDate.of(2024, 3, 20);

        // First, book Room B (capacity 20)
        Optional<MeetingRoom> bookedRoom1 = meetingRoomFinder.bookMeetingRoom(date, 15);
        assertTrue(bookedRoom1.isPresent(), "Room B should be available for the first booking");
        assertEquals(roomB, bookedRoom1.get(), "Room B should be the booked room");

        // Next, book Room C (capacity 30)
        Optional<MeetingRoom> bookedRoom2 = meetingRoomFinder.bookMeetingRoom(date, 25); // Needs capacity >= 25
        assertTrue(bookedRoom2.isPresent(), "Room C should be available for the second booking");
        assertEquals(roomC, bookedRoom2.get(), "Room C should be the booked room");

        // Try to book Room C again (should fail as it's already booked)
        Optional<MeetingRoom> bookedRoom3 = meetingRoomFinder.bookMeetingRoom(date, 25);
        assertFalse(bookedRoom3.isPresent(), "Room C should not be available for re-booking on the same day");

        // Try to book Room A for its capacity
        Optional<MeetingRoom> bookedRoom4 = meetingRoomFinder.bookMeetingRoom(date, 10);
        assertTrue(bookedRoom4.isPresent(), "Room A should be available for booking");
        assertEquals(roomA, bookedRoom4.get(), "Room A should be the booked room");

        // All rooms are now busy. Any further booking attempt should fail.
        Optional<MeetingRoom> bookedRoom5 = meetingRoomFinder.bookMeetingRoom(date, 5);
        assertFalse(bookedRoom5.isPresent(), "No more rooms should be available for booking on this date");
    }

    @Test
    @DisplayName("Should handle concurrent booking attempts correctly and ensure thread safety")
    void testThreadSafetyWithExpectedOutcomes() throws InterruptedException, ExecutionException {
        LocalDate date = LocalDate.of(2024, 3, 20);
        int numBookingAttempts = 100; // Increased attempts to better stress test

        // Using a ConcurrentHashMap to track results for robust assertion
        Set<MeetingRoom> successfullyBookedRooms = ConcurrentHashMap.newKeySet();
        List<Optional<MeetingRoom>> allResults = new ArrayList<>();

        // Create CompletableFuture tasks for concurrent booking
        List<CompletableFuture<Optional<MeetingRoom>>> futures = IntStream.range(0, numBookingAttempts)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 5 + (i * 5))))
                .collect(Collectors.toList());

        // Wait for all futures to complete and collect results
        for (CompletableFuture<Optional<MeetingRoom>> future : futures) {
            Optional<MeetingRoom> bookedRoom = future.get(); // .get() will block and rethrow exceptions
            allResults.add(bookedRoom); // Store all results (present or empty)
            bookedRoom.ifPresent(successfullyBookedRooms::add); // Add to set if present
        }

        // Assertions:
        // Since we have Room A, B, C, and requests for 5 people.
        // The first 3 successful attempts will book Room A, B, and C in order of capacity.
        // All subsequent attempts for the same date and capacity >= 5 should fail.
        assertAll(
            () -> assertEquals(3, successfullyBookedRooms.size(), "Only 3 rooms should be booked successfully (Room A, B, C)"),
            // Verify specific rooms were booked
            () -> assertTrue(successfullyBookedRooms.contains(roomA), "Room A should have been booked"),
            () -> assertTrue(successfullyBookedRooms.contains(roomB), "Room B should have been booked"),
            () -> assertTrue(successfullyBookedRooms.contains(roomC), "Room C should have been booked"),
            // Verify the total count of successful and failed bookings
            () -> assertEquals(3, allResults.stream().filter(Optional::isPresent).count(), "Exactly 3 bookings should be successful"),
            () -> assertEquals(numBookingAttempts - 3, allResults.stream().filter(Optional::isEmpty).count(), "The remaining bookings should have failed")
        );
    }

    @Test
    @DisplayName("Should allow booking the same meeting room on different dates")
    void testBookTheSameMeetingRoomOnDifferentDates() {
        LocalDate date1 = LocalDate.of(2024, 3, 20);
        LocalDate date2 = LocalDate.of(2024, 3, 21);
        LocalDate date3 = LocalDate.of(2024, 3, 22);

        // Book Room A for date1
        Optional<MeetingRoom> room1 = meetingRoomFinder.bookMeetingRoom(date1, 5);
        assertTrue(room1.isPresent(), "Room A should be booked for date1");
        assertEquals(roomA, room1.get(), "Room A should be the booked room for date1");

        // Book Room A for date2
        Optional<MeetingRoom> room2 = meetingRoomFinder.bookMeetingRoom(date2, 5);
        assertTrue(room2.isPresent(), "Room A should be booked for date2");
        assertEquals(roomA, room2.get(), "Room A should be the booked room for date2");

        // Book Room A for date3
        Optional<MeetingRoom> room3 = meetingRoomFinder.bookMeetingRoom(date3, 5);
        assertTrue(room3.isPresent(), "Room A should be booked for date3");
        assertEquals(roomA, room3.get(), "Room A should be the booked room for date3");

        // Verify they are indeed the same logical room (based on equals/hashCode from MeetingRoom)
        assertAll(
            () -> assertEquals(room1.get(), room2.get(), "The same room instance should be returned for different dates"),
            () -> assertEquals(room2.get(), room3.get(), "The same room instance should be returned for different dates")
        );

        // Book Room B for date1 (Room B should still be available for date1 since Room A was booked)
        Optional<MeetingRoom> roomB_date1 = meetingRoomFinder.bookMeetingRoom(date1, 15);
        assertTrue(roomB_date1.isPresent(), "Room B should be available for date1 after Room A was booked");
        assertEquals(roomB, roomB_date1.get(), "Room B should be the booked room for date1");
    }

    @Test
    @DisplayName("Should return empty optional if no room fits capacity")
    void testBookMeetingRoomNoFit() {
        LocalDate date = LocalDate.now();
        // Our max capacity is 30 (Room C). Request for 40 people.
        Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, 40);
        assertFalse(bookedRoom.isPresent(), "No room should be booked when requested capacity exceeds max capacity");
    }

    @Test
    @DisplayName("Should return empty optional for non-positive number of people")
    void testBookMeetingRoomNonPositivePeople() {
        LocalDate date = LocalDate.now();
        Optional<MeetingRoom> bookedRoomZero = meetingRoomFinder.bookMeetingRoom(date, 0);
        assertFalse(bookedRoomZero.isPresent(), "Booking should fail for zero people");

        Optional<MeetingRoom> bookedRoomNegative = meetingRoomFinder.bookMeetingRoom(date, -5);
        assertFalse(bookedRoomNegative.isPresent(), "Booking should fail for negative people");
    }
}
