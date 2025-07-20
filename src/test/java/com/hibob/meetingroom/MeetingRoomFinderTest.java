package com.hibob.meetingroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class MeetingRoomFinderTest {

    private MeetingRoomFinder meetingRoomFinder;

    @BeforeEach
    void setUp() {
        List<MeetingRoom> rooms = new ArrayList<>();
        rooms.add(new MeetingRoom(10, "Room A"));
        rooms.add(new MeetingRoom(30, "Room C"));
        rooms.add(new MeetingRoom(20, "Room B"));
        meetingRoomFinder = new MeetingRoomFinder(rooms);
    }

    @Test
    void testBookMeetingRoomSuccessfully() {
        LocalDate date = LocalDate.of(2024, 3, 20);
        Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, 15);

        assertTrue(bookedRoom.isPresent());
        assertEquals("Room B", bookedRoom.get().getRoomName());
    }

    @Test
    void testBookMeetingRoomNotAvailable() {
        LocalDate date = LocalDate.of(2024, 3, 20);
        meetingRoomFinder.bookMeetingRoom(date, 15); // Book Room B
        Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, 25); // Try to book Room C

        assertTrue(bookedRoom.isPresent());
        assertEquals("Room C", bookedRoom.get().getRoomName());
        
        bookedRoom = meetingRoomFinder.bookMeetingRoom(date, 25); // Try to book Room C again
        assertFalse(bookedRoom.isPresent()); // Should not be available
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        LocalDate date = LocalDate.of(2024, 3, 20);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1);
        int numberOfThreads = 10;

        for (int i = 0; i < numberOfThreads; i++) {
            final int numOfPeople = 5 + (i * 5); // Varying number of people
            executor.submit(() -> {
                try {
                    latch.await(); // Wait for the latch to be released
                    Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, numOfPeople);
                    assertNotNull(bookedRoom);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown(); // Release all threads
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES)); // Wait for all threads to finish
    }
    
    @Test
    void testBookMeetingRoomConcurrentAccess() throws InterruptedException, ExecutionException {

        LocalDate date = LocalDate.of(2024, 3, 20);

        // Create a list to hold CompletableFuture objects for booking rooms concurrently
        List<CompletableFuture<Optional<MeetingRoom>>> futuresList = new ArrayList<>();

        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 50)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 10)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 10)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 15)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 15)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 20)));
        futuresList.add(CompletableFuture.supplyAsync(() -> meetingRoomFinder.bookMeetingRoom(date, 20)));

        // Create an array of the correct type
        @SuppressWarnings("unchecked") // Suppress the unchecked warning
        CompletableFuture<Optional<MeetingRoom>>[] futures = (CompletableFuture<Optional<MeetingRoom>>[]) new CompletableFuture<?>[futuresList.size()];

        // Fill the array with the contents of the list
        futuresList.toArray(futures);

        // You can then use CompletableFuture.allOf to wait for all futures to complete if needed
        CompletableFuture.allOf(futures);
        
        int actualNumOfSuccessBookMeetingRooms = 0;
        int actualNumOfFailBookMeetingRooms = 0;
        
        int expectedNumOfSuccessBookMeetingRooms = 3;
        int expectedNumOfFailBookMeetingRooms = 4;
        
        for (CompletableFuture<Optional<MeetingRoom>> future : futures) {

            if (future.get().isPresent()) {
                
                ++actualNumOfSuccessBookMeetingRooms;
                
            } else {

                ++actualNumOfFailBookMeetingRooms;
            }
        }

        assertEquals(expectedNumOfSuccessBookMeetingRooms, actualNumOfSuccessBookMeetingRooms, "The actual value should equal the expected value.");
        assertEquals(expectedNumOfFailBookMeetingRooms, actualNumOfFailBookMeetingRooms, "The actual value should equal the expected value.");
    }
    
    @Test
    void testThreadSafetyWithExpectedOutcomes() throws InterruptedException {
        LocalDate date = LocalDate.of(2024, 3, 20);
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        Set<MeetingRoom> bookedRooms = ConcurrentHashMap.newKeySet(); // Thread-safe set to track booked rooms

        List<CompletableFuture<Optional<MeetingRoom>>> futuresList = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int numOfPeople = 5 + (i * 5); // Varying number of people
            futuresList.add(CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(); // Wait for the latch to be released
                    Optional<MeetingRoom> bookedRoom = meetingRoomFinder.bookMeetingRoom(date, numOfPeople);
                    bookedRoom.ifPresent(bookedRooms::add); // Add to booked rooms if present
                    return bookedRoom;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }, executor));
        }

        latch.countDown(); // Release all threads
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS)); // Wait for all threads to finish

        log.debug("bookedRooms.size()={}", bookedRooms.size());

        // Check that the number of booked rooms is as expected
        assertTrue(bookedRooms.size() == 3, "No more rooms should be booked than the total rooms exist.");
    }

    @Test
    void testBookTheSameMeetingRoomOnDifferentDates() {
        LocalDate date1 = LocalDate.of(2024, 3, 20);
        LocalDate date2 = LocalDate.of(2024, 3, 21);

        Optional<MeetingRoom> room1 = meetingRoomFinder.bookMeetingRoom(date1, 10);
        Optional<MeetingRoom> room2 = meetingRoomFinder.bookMeetingRoom(date2, 10);

        assertTrue(room1.isPresent());
        assertTrue(room2.isPresent());
        assertEquals(room1.get(), room2.get(), "Ensure the same room can be booked on different dates");
    }
}
