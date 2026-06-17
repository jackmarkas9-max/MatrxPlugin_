package net.matrx.plugin.managers;

import net.matrx.plugin.models.*;
import net.matrx.plugin.storage.DataStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public class CourseManager {
    private final DataStorage storage;
    private final Map<String, Course> courses;
    private final Map<String, List<double[]>> ghostRecordings;

    public CourseManager(DataStorage storage) {
        this.storage = storage;
        this.courses = new LinkedHashMap<>();
        this.ghostRecordings = new HashMap<>();
        loadCourses();
    }

    public void loadCourses() {
        courses.clear();
        courses.putAll(storage.loadCourses());
        if (courses.isEmpty()) {
            storage.saveDefaultCourses();
        }
    }

    public void saveCourses() {
        storage.saveCourses(courses);
    }

    public Map<String, Course> getCourses() { return courses; }
    public Course getCourse(String name) {
        Course c = courses.get(name.toLowerCase());
        if (c != null) return c;
        try {
            int id = Integer.parseInt(name);
            return getCourseById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    public Course getCourseById(int id) {
        return courses.values().stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }
    public void addCourse(Course course) { courses.put(course.getName(), course); saveCourses(); }
    public void removeCourse(String name) { courses.remove(name.toLowerCase()); saveCourses(); }

    public boolean isInCourse(Player player, ParkourPlayer pPlayer) {
        return pPlayer.getActiveCourse() != null;
    }

    public Checkpoint findCheckpoint(Player player, ParkourPlayer pPlayer) {
        if (pPlayer.getActiveCourse() == null) return null;
        Course course = getCourse(pPlayer.getActiveCourse());
        if (course == null) return null;
        for (Checkpoint cp : course.getCheckpoints()) {
            if (cp.matchesBlock(player.getLocation())) return cp;
        }
        return null;
    }

    public int findCheckpointIndex(Player player, ParkourPlayer pPlayer) {
        if (pPlayer.getActiveCourse() == null) return -1;
        Course course = getCourse(pPlayer.getActiveCourse());
        if (course == null) return -1;
        for (int i = 0; i < course.getCheckpoints().size(); i++) {
            if (course.getCheckpoints().get(i).matchesBlock(player.getLocation())) return i;
        }
        return -1;
    }

    public Course.HiddenStar findHiddenStar(Player player, ParkourPlayer pPlayer) {
        if (pPlayer.getActiveCourse() == null) return null;
        Course course = getCourse(pPlayer.getActiveCourse());
        if (course == null) return null;
        return course.findStarAt(player.getLocation());
    }

    public boolean isAtStart(Player player) {
        for (Course course : courses.values()) {
            if (course.getStart() != null && course.getStart().matchesBlock(player.getLocation())) {
                return true;
            }
        }
        return false;
    }

    public Course getCourseAtStart(Player player) {
        for (Course course : courses.values()) {
            if (course.getStart() != null && course.getStart().matchesBlock(player.getLocation())) {
                return course;
            }
        }
        return null;
    }

    public boolean isAtEnd(Player player, ParkourPlayer pPlayer) {
        if (pPlayer.getActiveCourse() == null) return false;
        Course course = getCourse(pPlayer.getActiveCourse());
        if (course == null) return false;
        return course.getEnd() != null && course.getEnd().matchesBlock(player.getLocation());
    }

    public long finishCourse(Player player, ParkourPlayer pPlayer) {
        if (pPlayer.getActiveCourse() == null) return -1;
        Course course = getCourse(pPlayer.getActiveCourse());
        if (course == null) return -1;

        long elapsed = pPlayer.getElapsedTime();
        if (elapsed < 0) return -1;

        pPlayer.setBestTime(course.getName(), elapsed);
        pPlayer.finishCourse();
        return elapsed;
    }

    public void saveGhostRecording(String courseName, long time, List<double[]> frames) {
        String key = courseName + ":" + time;
        ghostRecordings.put(key, frames);
    }

    public List<double[]> getGhostRecording(String courseName, long time) {
        return ghostRecordings.get(courseName + ":" + time);
    }

    public List<int[]> getCourseTiers() {
        Map<Integer, List<String>> tierMap = new TreeMap<>();
        for (Course course : courses.values()) {
            tierMap.computeIfAbsent(course.getTier(), k -> new ArrayList<>()).add(course.getName());
        }
        List<int[]> result = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : tierMap.entrySet()) {
            result.add(new int[]{entry.getKey(), entry.getValue().size()});
        }
        return result;
    }
}
