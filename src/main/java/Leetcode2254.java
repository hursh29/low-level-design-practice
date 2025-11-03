import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

class Video {
    private int videoId;
    private String videoContent;

    private int likes;

    public int getLikes() {
        return likes;
    }

    public int getDisLikes() {
        return disLikes;
    }

    public int getViews() {
        return views;
    }

    private int disLikes;
    private int views;

    Video(int id, String videoContent) {
        this.videoId = id;
        this.videoContent = videoContent;
        this.likes = 0;
        this.disLikes = 0;
        this.views = 0;
    }

    public void incrementLikes() {
        this.likes += 1;
    }

    public void incrementDisLikes() {
        this.disLikes += 1;
    }

    public void incrementViews() {
        this.views += 1;
    }

    public String watchVideoContent(final int L, final int R) {
        final int correctR = Integer.min(R, videoContent.length() - 1);

        return videoContent.substring(L, correctR);
    }
}

class IdManager {

    private final TreeSet<Integer> takenIds;
    private final TreeSet<Integer> removedIds;

    IdManager() {
        this.takenIds = new TreeSet<>();
        this.removedIds = new TreeSet<>();
    }

    public Integer getNotTakenId() {
        if (removedIds.isEmpty()) {
            return takenIds.size();
        } else {
            final var minimumRemovedId = removedIds.pollFirst();
            removedIds.remove(minimumRemovedId);

            takenIds.add(minimumRemovedId);
            return minimumRemovedId;
        }
    }

    public void addToRemoved(final Integer inputId) {
        removedIds.add(inputId);
    }

    public void addToTakenIds(final Integer inputId) {
        takenIds.add(inputId);
    }
}

class VideoSharingPlatform {
    private final Map<Integer, Video> videoIdToVideo;
    private final IdManager idManager;

    public VideoSharingPlatform() {
        idManager = new IdManager();
        videoIdToVideo = new HashMap<>();
    }

    public int upload(String video) {
        final var newVideoId = idManager.getNotTakenId();
        final var newVideo = new Video(newVideoId, video);

        videoIdToVideo.put(newVideoId, newVideo);

        idManager.addToTakenIds(newVideoId);

        return newVideoId;
    }

    public void remove(int videoId) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);

        if (mayBeVideo == null) {
            return;
        }

        videoIdToVideo.remove(videoId);
        idManager.addToRemoved(videoId);
    }

    public String watch(int videoId, int startMinute, int endMinute) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);

        if (mayBeVideo == null) {
            return "-1";
        }

        mayBeVideo.incrementViews();
        return mayBeVideo.watchVideoContent(startMinute, endMinute);
    }

    public void like(int videoId) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);

        if (mayBeVideo == null) {
            return ;
        }

        mayBeVideo.incrementLikes();
    }

    public void dislike(int videoId) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);

        if (mayBeVideo == null) {
            return;
        }

        mayBeVideo.incrementDisLikes();
    }

    public int[] getLikesAndDislikes(int videoId) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);
        final var answer = new int[2];
        if (mayBeVideo == null) {
            answer[0] = -1;
            answer[1] = -1;
            return answer;
        }

        answer[0] = mayBeVideo.getLikes();
        answer[1] = mayBeVideo.getDisLikes();

        return answer;
    }

    public int getViews(int videoId) {
        final var mayBeVideo = this.videoIdToVideo.get(videoId);

        if (mayBeVideo == null) {
            return -1;
        }

        return mayBeVideo.getViews();
    }
}

public class Leetcode2254 {

}
