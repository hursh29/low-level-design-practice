
// requirements
// - admin creates show based on a combination of Movie and cinemaHall (N number of shows)
// - users should be able to search by location, and movie names
// - users should be able to view shows, and seats for a given show
// - users should be able to  book tickets for a show
// - users should be able to pay for the booked tickets ??? very tough

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

class Actor {
    protected String id;
    protected String name;
    protected String mailId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMailId() {
        return mailId;
    }

    public void setMailId(String mailId) {
        this.mailId = mailId;
    }

    public Actor(String id, String name, String mailId) {
        this.id = id;
        this.name = name;
        this.mailId = mailId;
    }
}

class Admin extends Actor {
    public Admin(String id, String name, String mailId) {
        super(id, name, mailId);
    }
}

class BookingAudit {
    private String userId;
    private String seatId;
    private String showId;

    public BookingAudit(String userId, String seatId, String showId) {
        this.userId = userId;
        this.seatId = seatId;
        this.showId = showId;
    }
}

class User extends Actor {
     private final List<BookingAudit> bookingAudit;

    public User(String id, String name, String mailId) {
        super(id, name, mailId);
        this.bookingAudit = new ArrayList<>();
    }

    public void addBookingAudit(final BookingAudit audit) {
        this.bookingAudit.add(audit);
    }
}

enum MovieGenre {
    SPACE, SCIENCE_FICTION, GORE, ACTION, ROMANCE
}

class Movie {
    @Override
    public String toString() {
        return "Movie{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", genre=" + genre +
            '}';
    }

    private String id;
    private String name;
    private MovieGenre genre;
    private Double rating;
    private Double lengthInMinutes;
    private List<String> actors;
    private List<String> producers;

    public String getId() {
        return id;
    }

    public Movie(String id,
        String name, MovieGenre genre, Double rating,
                 Double lengthInMinutes, List<String> actors, List<String> producers) {
        this.id = id;
        this.name = name;
        this.genre = genre;
        this.rating = rating;
        this.lengthInMinutes = lengthInMinutes;
        this.actors = actors;
        this.producers = producers;
    }

    public String getName() {
        return name;
    }

    public MovieGenre getGenre() {
        return genre;
    }

    public Double getRating() {
        return rating;
    }

    public Double getLengthInMinutes() {
        return lengthInMinutes;
    }

    public List<String> getActors() {
        return actors;
    }

    public List<String> getProducers() {
        return producers;
    }
}

class MovieManager {
    private final Map<String, Movie> movieIdToMovie;

    public MovieManager() {
        this.movieIdToMovie = new HashMap<>();
    }

    public void onboardMovie(
        String name, MovieGenre genre, Double rating,
        Double lengthInMinutes, List<String> actors, List<String> producers) {
        final var newMovieId = String.valueOf(movieIdToMovie.size());

        final var newMovie = new Movie(newMovieId, name, genre, rating, lengthInMinutes, actors, producers);

        movieIdToMovie.put(newMovieId, newMovie);
    }

    public Movie getMovieById(final String id) {
        return Optional.ofNullable(movieIdToMovie.get(id))
            .orElseThrow(() -> new IllegalStateException("Movie not found with id" + id ));
    }
}

class Address {
    private final String mainAddress;
    private final Long zipcode;
    private final String city;
    private final String state;
    private final Double longtitude;
    private final Double latitude;

    public Address(String mainAddress, Long zipcode, String city, String state, Double longtitude, Double latitude) {
        this.mainAddress = mainAddress;
        this.zipcode = zipcode;
        this.city = city;
        this.state = state;
        this.longtitude = longtitude;
        this.latitude = latitude;
    }

    public Double getLongtitude() {
        return longtitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public Long getZipcode() {
        return zipcode;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

}

enum SeatStatus {
    BOOKED, AVAILABLE, DISABLED
}

class Seat {
    @Override
    public String toString() {
        return "Seat{" +
            "id='" + id + '\'' +
            ", status=" + status +
            '}';
    }

    private final String id;
    private SeatStatus status;

    public String getId() {
        return id;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Seat(String id, SeatStatus status) {
        this.id = id;
        this.status = status;
    }

    public void bookSeat() {
        this.status = SeatStatus.BOOKED;
    }
}

class Hall {
    private final String id;

    public String getId() {
        return id;
    }

    public long getSeatingCapacity() {
        return seatingCapacity;
    }

    private final long seatingCapacity;

    public Hall(String id, long seatingCapacity) {
        this.id = id;
        this.seatingCapacity = seatingCapacity;
    }

    @Override
    public String toString() {
        return "Hall{" +
            "id='" + id + '\'' +
            ", seatingCapacity=" + seatingCapacity +
            '}';
    }
}

class Theatre {
    private final String id;
    private final String name;
    private final Address location;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Address getLocation() {
        return location;
    }

    private final List<Hall> halls;

    public List<Hall> getHalls() {
        return halls;
    }

    public Theatre(String id, String name, Address location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.halls = new ArrayList<>();
    }

    public void addHall(final long capacity) {
        final var compositeHallId = String.format("%s_%s", this.id, halls.size());

        halls.add(new Hall(compositeHallId, capacity));
    }

    public Hall getHall(final String hallId) {
        return halls.stream()
            .filter(hall -> hallId.equals(hall.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Hall not found"));
    }
}

class TheatreManager {
    private final Map<String, Theatre> theatreIdToTheatre;

    public TheatreManager() {
        this.theatreIdToTheatre = new HashMap<>();
    }

    public void onboardTheatre(final String name, final Address location) {
        final var newTheatreId = String.valueOf(theatreIdToTheatre.size());

        final var newTheatre = new Theatre(newTheatreId, name, location);
        newTheatre.addHall(20L);
        newTheatre.addHall(5L);

        theatreIdToTheatre.put(newTheatreId, newTheatre);
    }

    public Theatre getTheatreById(final String id) {
        return Optional.ofNullable(theatreIdToTheatre.get(id))
            .orElseThrow(() -> new IllegalStateException("Theatre not found with id" + id));
    }

    public Hall getHallById(final String theatreId, final String compositeId) {
        return this.getTheatreById(theatreId).getHall(compositeId);
    }

    public List<Theatre> getAllHallsWithMatchingCityName(final String cityNamePrefix) {
        return this.theatreIdToTheatre.values().stream()
            .filter(theatre -> theatre.getLocation().getCity().startsWith(cityNamePrefix))
            .collect(Collectors.toList());
    }

    public List<Theatre> getAllHallsWithZipCode(final Long zipCode) {
        return this.theatreIdToTheatre.values().stream()
            .filter(theatre -> theatre.getLocation().getZipcode().equals(zipCode))
            .collect(Collectors.toList());
    }

}

class Show {
    @Override
    public String toString() {
        return "Show{" +
            "id='" + id + '\'' +
            ", movie=" + movie +
            ", playingHall=" + playingHall +
            '}';
    }

    private final String id; // movie + hallId
    private final Movie movie;
    private final Hall playingHall;
    private final List<Seat> seats;

    public Seat bookSeat(final String seatId) {
        final var relevantSeat = this.seats.stream()
            .filter(seat -> seatId.equals(seat.getId()))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        relevantSeat.bookSeat();

        return relevantSeat;
    }

    public String getId() {
        return id;
    }

    public Movie getMovie() {
        return movie;
    }

    public Hall getPlayingHall() {
        return playingHall;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public Show(Movie movie, Hall playingHall) {
        this.id = String.format("%s_%s", movie.getId(), playingHall.getId());
        this.movie = movie;
        this.playingHall = playingHall;
        this.seats = LongStream.range(0L, playingHall.getSeatingCapacity())
            .mapToObj(seatId -> new Seat(String.valueOf(seatId), SeatStatus.AVAILABLE))
            .collect(Collectors.toList());
    }
}

class ShowManager {
    private final MovieManager movieManager;
    private final TheatreManager theatreManager;
    private List<Show> shows;

    public ShowManager(final MovieManager movieManager, final TheatreManager theatreManager) {
        this.shows = new ArrayList<>();
        this.movieManager = movieManager;
        this.theatreManager = theatreManager;
    }

    public Show createShow(final String movieId, final String theatreId, final String hallId) {
        final var movie = movieManager.getMovieById(movieId);
        final var theatreHall = theatreManager.getHallById(theatreId, hallId);

        final var newShow = new Show(movie, theatreHall);

        shows.add(newShow);

        return newShow;
    }

    public List<Show> findShowByMovieName(final String movieName) {
        return this.shows.stream()
            .filter(show -> show.getMovie().getName().startsWith(movieName))
            .collect(Collectors.toList());
    }

    public List<Show> findShowByGenre(final MovieGenre movieGenre) {
        return this.shows.stream()
            .filter(show -> show.getMovie().getGenre().equals(movieGenre))
            .collect(Collectors.toList());
    }

    public List<Show> findShowByLocation(final String cityName) {
        final var relevantHalls = theatreManager.getAllHallsWithMatchingCityName(cityName).stream()
            .map(Theatre::getHalls)
            .flatMap(Collection::stream)
            .map(Hall::getId)
            .collect(Collectors.toList());

        return this.shows.stream()
            .filter(show -> relevantHalls.contains(show.getPlayingHall().getId()))
            .collect(Collectors.toList());
    }

    public Map<SeatStatus, Long> printSeatMap(final String showId) {
        final var relevantShow = shows.stream()
            .filter(show -> show.getId().equals(showId))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        final var seatMap = relevantShow.getSeats().stream()
            .collect(Collectors.groupingBy(Seat::getStatus, Collectors.counting()));

        System.out.println(seatMap);

        return seatMap;
    }

    public Seat bookSeat(final String showId, final String seatId, final User user) {
        final var relevantShow = shows.stream()
            .filter(show -> show.getId().equals(showId))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        final var newBookingAudit = new BookingAudit(user.getId(), seatId, showId);

        user.addBookingAudit(newBookingAudit);

        return relevantShow.bookSeat(seatId);
    }
}

public class LowLevelDesignBookMyShow {

    public static void main(String[] args) {
        final var movieManager = new MovieManager();
        final var theatreManager = new TheatreManager();

        movieManager.onboardMovie("Veer zara",
            MovieGenre.ROMANCE, 9.9, 120.2,
            List.of("Shahrukh Khan", "Priety Zinta"),
            List.of("Yash Raj Films")
            );
        movieManager.onboardMovie("Avengers Age of Ultron",
            MovieGenre.ACTION, 9.2, 101.2,
            List.of("Scarlett Johanson", "Tony Stark"),
            List.of("Stark productions")
        );

        final var address1 = new Address("Crown Plaza Mall",
            121009L,
            "Faridabad",
            "Haryana", 1231.1233, -87.123);

        theatreManager.onboardTheatre("Crown Plaza Mall", address1);
        theatreManager.getTheatreById("0").addHall(20);

        final var showManager = new ShowManager(movieManager, theatreManager);

        final var createdShow = showManager.createShow("0", "0", "0_0");
        final var createdShow2 = showManager.createShow("1", "0", "0_1");

        showManager.findShowByGenre(MovieGenre.ACTION).forEach(System.out::println);

        showManager.findShowByMovieName("Veer").forEach(System.out::println);

        showManager.printSeatMap(createdShow.getId());
        final var user = new User("123", "Steve Rogers", "rogers@gmail.com");
        showManager.bookSeat(createdShow.getId(), "12", user);

        showManager.printSeatMap(createdShow.getId());
    }
}
