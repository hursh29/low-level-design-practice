import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

enum Genre {
    JAZZ, POP, HIPHOP, CLASSIC
}

class Song {
    private final String id;
    private final String name;
    private final String artist;
    private final Genre genre;

    public Song(String id, String name, String artist, Genre genre) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArtist() {
        return artist;
    }

    public void playSong() {
        System.out.println("Playing song = " + this.getName() + " by artist " + this.getArtist());
    }
}

class Playlist {
    private final String id;
    private final String name;
    private final List<Song> songs;
    private Integer currentTracker;
    private boolean repeatEnabled;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
        this.songs = new ArrayList<>();
        this.currentTracker = 0;
        this.repeatEnabled = false;
    }

    public void resetCurrentTracker() {
        this.currentTracker = 0;
    }

    public void addSongToPlaylist(final Song song) {
        songs.add(song);
    }

    public void removeSongFromPlaylist(final String songId) {
        songs.removeIf(song -> song.getId().equals(songId));
    }

    public Song getNextSong() {
        if (this.songs.isEmpty()) {
            throw new IllegalStateException("Cannt play there are no songs as of now");
        }
        final var nextSongId = repeatEnabled ? (currentTracker + 1) % songs.size() : Integer.min(
            songs.size() - 1, currentTracker + 1);
        this.currentTracker = nextSongId;
        return songs.get(nextSongId);
    }

    public void toggleRepeatMode() {
        this.repeatEnabled = !this.repeatEnabled;
    }
}

class MusicPlayer {
    private final Map<String, Playlist> playlistIdToPlaylists;

    MusicPlayer() {
        playlistIdToPlaylists = new HashMap<>();
    }

    private Optional<Playlist> getPlaylist(String playlistId) {
        return Optional.ofNullable(playlistIdToPlaylists.get(playlistId));
    }

    public void createPlaylist(String playlistId, String playlistName) {
        if (playlistIdToPlaylists.containsKey(playlistId)) {
            throw new IllegalStateException("Playlist already exists");
        }

        final var newPlaylist = new Playlist(playlistId, playlistName);

        playlistIdToPlaylists.put(playlistId, newPlaylist);
    }

    public void addSongToPlaylist(final String playlistId, final Song song) {
        getPlaylist(playlistId)
            .ifPresent(playlist -> playlist.addSongToPlaylist(song));
    }

    public void removeSongFromPlaylist(final String playlistId, final String songId) {
        getPlaylist(playlistId)
            .ifPresent(playlist -> {
                playlist.removeSongFromPlaylist(songId);
                playlist.resetCurrentTracker();
            });
    }

    public Song getNextSong(final String playlistId) {
        final var nextSong = getPlaylist(playlistId)
            .map(Playlist::getNextSong);

        nextSong.ifPresent(Song::playSong);

        return nextSong.orElse(null);
    }

    public void repeatPlaylist(final String playlistId) {
        getPlaylist(playlistId)
            .ifPresent(Playlist::toggleRepeatMode);
    }

}

public class LowLevelDesignMusicPlaylist {

    public static void main(String[] args) {
        final var musicPlayer = new MusicPlayer();

        final var song1 = new Song("0", "Skyfall", "Adele", Genre.POP);
        final var song2 = new Song("1", "Alone", "Marshmello", Genre.POP);
        final var song3 = new Song("2", "Sing me to sleep", "Alan Walker", Genre.POP);
        final var song4 = new Song("3", "Faded", "Alan Walker", Genre.POP);
        final var song5 = new Song("4", "Middle of the night", "Zara Larrson", Genre.POP);

        musicPlayer.createPlaylist("1",  "Hursh's Favourite");
        musicPlayer.addSongToPlaylist("1", song1);
        musicPlayer.addSongToPlaylist("1", song2);
        musicPlayer.addSongToPlaylist("1", song3);

//        musicPlayer.repeatPlaylist("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");

        System.out.println("---------\n");
        musicPlayer.repeatPlaylist("1");
        musicPlayer.removeSongFromPlaylist("1", "2");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");


        System.out.println("---------\n");

        musicPlayer.addSongToPlaylist("1", song5);
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");
        musicPlayer.getNextSong("1");

        System.out.println("---------\n");
    }
}
