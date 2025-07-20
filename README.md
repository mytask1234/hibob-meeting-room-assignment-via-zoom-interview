## hibob-meeting-room-assignment-via-zoom-interview

During the Zoom interview with 2 technical persons from HiBob company, they sent me a link (via the Zoom chat) to an online IDE on [codesignal.com](https://codesignal.com/), and gave me the assignment to implement.  
You can choose whatever language you want, I chose Java.

So, in their predefined java project, there are 2 classes:  
  
**MeetingRoom.java**  

```java
public class MeetingRoom {

    int capacity;
    String roomName;
}
```

and **MeetingRoomFinder.java**  

```java
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MeetingRoomFinder {

    public List<MeetingRoom> meetingRooms;

    public MeetingRoomFinder(List<MeetingRoom> meetingRooms) {
        this.meetingRooms = meetingRooms;
    }

    public Optional<MeetingRoom> bookMeetingRoom(LocalDate date, int numOfPeople) {
        return null;
    }
}
```

and they asked me to implement the method **bookMeetingRoom** to return an available room for that date, with the smallest capacity that sufficient for that number of people.  

In this GitHub project you can find my solution, which is thread-safe (support concurrency), and also 2 classes of unit-tests.  

I recommend you to download this project, run the unit-tests, and use AI tools like GPT to better understand the code.  

N.B. they asked me not to use the internet at all, while I solve the assignment.  

Feel free to share your comments on the gist page [here](https://gist.github.com/mytask1234/7ce916d5190f562f80b2c3e760ce7f39).  

Hope it helped and Good Luck :)
