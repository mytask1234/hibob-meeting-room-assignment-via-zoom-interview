package com.hibob.meetingroom;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class MeetingRoom implements Comparable<MeetingRoom> {

    @EqualsAndHashCode.Exclude
    private final int capacity;

    @EqualsAndHashCode.Include
    private final String roomName;

    public MeetingRoom(int capacity, String roomName) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.roomName = roomName;
    }

    @Override
    public int compareTo(MeetingRoom other) {
        return Integer.compare(this.capacity, other.capacity);
    }
}
