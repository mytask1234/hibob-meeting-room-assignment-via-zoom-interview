package com.hibob.meetingroom;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MeetingRoomFinder {

    private final Set<MeetingRoom> allRoomsReadOnlySet;
    private final int maxRoomCapacity;
    private final ConcurrentHashMap<LocalDate, Set<MeetingRoom>> dateToBusyRoomsMap;

    public MeetingRoomFinder(List<MeetingRoom> meetingRooms) {
        // Use Objects.requireNonNull for better null check messaging and conciseness
        Objects.requireNonNull(meetingRooms, "Parameter meetingRooms cannot be null");

        // The original code had a potential NoSuchElementException if meetingRooms is empty.
        // It's good practice to handle this explicitly, as last() on an empty set throws.
        if (meetingRooms.isEmpty()) {
            throw new IllegalArgumentException("The list of meeting rooms cannot be empty.");
        }

        // Use a temporary ConcurrentSkipListSet for sorting and getting the last element.
        // This ensures the set is sorted for maxCapacity and then made unmodifiable.
        NavigableSet<MeetingRoom> tempSortedRooms = new ConcurrentSkipListSet<>(meetingRooms);

        this.maxRoomCapacity = tempSortedRooms.last().getCapacity();
        // Collections.unmodifiableSortedSet returns a SortedSet, which NavigableSet extends.
        this.allRoomsReadOnlySet = Collections.unmodifiableSortedSet(tempSortedRooms);
        this.dateToBusyRoomsMap = new ConcurrentHashMap<>();

        log.debug("allRoomsReadOnlySet={}", allRoomsReadOnlySet);
        log.debug("maxRoomCapacity={}", maxRoomCapacity);
    }

    /**
     * Attempts to book an available meeting room for a given date and number of people.
     * The method finds the smallest capacity room that can accommodate the number of people
     * and is not already busy on that date. If a room is found and successfully booked,
     * it is added to the set of busy rooms for that date.
     *
     * @param date The date for which to book the meeting room.
     * @param numOfPeople The number of people attending the meeting.
     * @return An Optional containing the booked MeetingRoom if successful, or an empty Optional otherwise.
     */
    public Optional<MeetingRoom> bookMeetingRoom(LocalDate date, int numOfPeople) {
        Objects.requireNonNull(date, "Date cannot be null");

        if (numOfPeople <= 0) {
            log.warn("Attempted to book a room for non-positive number of people: {}", numOfPeople);
            return Optional.empty(); // Or throw IllegalArgumentException, depending on desired behavior
        }

        if (numOfPeople > maxRoomCapacity) {
            log.info("No room available for {} people, exceeding max capacity of {}", numOfPeople, maxRoomCapacity);
            return Optional.empty();
        }

        // computeIfAbsent is thread-safe and creates the set if not present.
        // We use ConcurrentHashMap.newKeySet() which returns a ConcurrentHashMap.KeySetView,
        // which is a concurrent Set implementation.
        Set<MeetingRoom> busyRoomsForDate = dateToBusyRoomsMap.computeIfAbsent(date, k -> ConcurrentHashMap.newKeySet());

        return findAvailableRoom(busyRoomsForDate, numOfPeople);
    }

    /**
     * Helper method to find and attempt to book an available room.
     * It iterates through all rooms, filters out busy ones and those too small,
     * and attempts to add the first suitable room to the busy set.
     *
     * @param busyRoomsForDate The set of rooms already booked for the given date.
     * @param numOfPeople The number of people to accommodate.
     * @return An Optional containing the found and booked MeetingRoom, or empty if none found or booking failed.
     */
    private Optional<MeetingRoom> findAvailableRoom(Set<MeetingRoom> busyRoomsForDate, int numOfPeople) {
        return allRoomsReadOnlySet.stream()
                .filter(room -> !busyRoomsForDate.contains(room)) // Filter out already busy rooms
                .filter(room -> room.getCapacity() >= numOfPeople) // Filter out rooms that are too small
                .findFirst() // Get the first suitable room (which will be the smallest due to sorted set)
                .filter(busyRoomsForDate::add); // Atomically try to add the room to the busy set.
                                                // Returns true if added (i.e., it wasn't already there).
                                                // This is a crucial part for concurrency handling.
    }
}
