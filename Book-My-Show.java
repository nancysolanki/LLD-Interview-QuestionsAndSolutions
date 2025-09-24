import java.util.*;

// -----------------------------
// Enums (foundation)
// -----------------------------
public enum City {
    Bangalore,
    Delhi;
}

public enum SeatCategory {
    SILVER,
    GOLD,
    PLATINUM;
}

// -----------------------------
// Low-level models (seats & screens)
// -----------------------------
public class Seat {
    int seatId;
    int row;
    SeatCategory seatCategory;

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public SeatCategory getSeatCategory() { return seatCategory; }
    public void setSeatCategory(SeatCategory seatCategory) { this.seatCategory = seatCategory; }
}

public class Screen {
    int screenId;
    List<Seat> seats = new ArrayList<>();

    public int getScreenId() { return screenId; }
    public void setScreenId(int screenId) { this.screenId = screenId; }
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
}

// -----------------------------
// Mid-level models (movie, show, theatre)
// -----------------------------
public class Movie {
    int movieId;
    String movieName;
    int movieDurationInMinutes;
    // other details like Genre, Language etc.

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }
    public int getMovieDuration() { return movieDurationInMinutes; }
    public void setMovieDuration(int movieDuration) { this.movieDurationInMinutes = movieDuration; }
}

public class Show {
    int showId;
    Movie movie;
    Screen screen;
    int showStartTime;
    List<Integer> bookedSeatIds = new ArrayList<>();

    public int getShowId() { return showId; }
    public void setShowId(int showId) { this.showId = showId; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }
    public int getShowStartTime() { return showStartTime; }
    public void setShowStartTime(int showStartTime) { this.showStartTime = showStartTime; }
    public List<Integer> getBookedSeatIds() { return bookedSeatIds; }
    public void setBookedSeatIds(List<Integer> bookedSeatIds) { this.bookedSeatIds = bookedSeatIds; }
}

public class Theatre {
    int theatreId;
    String address;
    City city;
    List<Screen> screen = new ArrayList<>();
    List<Show> shows = new ArrayList<>();

    public int getTheatreId() { return theatreId; }
    public void setTheatreId(int theatreId) { this.theatreId = theatreId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public List<Screen> getScreen() { return screen; }
    public void setScreen(List<Screen> screen) { this.screen = screen; }
    public List<Show> getShows() { return shows; }
    public void setShows(List<Show> shows) { this.shows = shows; }
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}

// -----------------------------
// Payment & Booking (higher-level models)
// -----------------------------
public class Payment {
    int paymentId;
    // Other payment details
}

public class Booking {
    Show show;
    List<Seat> bookedSeats = new ArrayList<>();
    Payment payment;

    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }
    public List<Seat> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<Seat> bookedSeats) { this.bookedSeats = bookedSeats; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
}

// -----------------------------
// Controllers (movie & theatre management)
// -----------------------------
public class MovieController {
    Map<City, List<Movie>> cityVsMovies;
    List<Movie> allMovies;

    MovieController() {
        cityVsMovies = new HashMap<>();
        allMovies = new ArrayList<>();
    }

    // ADD movie to a particular city
    void addMovie(Movie movie, City city) {
        allMovies.add(movie);
        List<Movie> movies = cityVsMovies.getOrDefault(city, new ArrayList<>());
        movies.add(movie);
        cityVsMovies.put(city, movies);
    }

    // GET movie by name
    Movie getMovieByName(String movieName) {
        for (Movie movie : allMovies) {
            if ((movie.getMovieName()).equals(movieName)) {
                return movie;
            }
        }
        return null;
    }

    // GET movies by city
    List<Movie> getMoviesByCity(City city) {
        return cityVsMovies.get(city);
    }

    // TODO: REMOVE, UPDATE and CRUD by ID (left as comments as in original)
}

public class TheatreController {
    Map<City, List<Theatre>> cityVsTheatre;
    List<Theatre> allTheatre;

    TheatreController() {
        cityVsTheatre = new HashMap<>();
        allTheatre = new ArrayList<>();
    }

    void addTheatre(Theatre theatre, City city) {
        allTheatre.add(theatre);
        List<Theatre> theatres = cityVsTheatre.getOrDefault(city, new ArrayList<>());
        theatres.add(theatre);
        cityVsTheatre.put(city, theatres);
    }

    // Returns mapping of theatres->shows for a movie in a city
    Map<Theatre, List<Show>> getAllShow(Movie movie, City city) {
        Map<Theatre, List<Show>> theatreVsShows = new HashMap<>();
        List<Theatre> theatres = cityVsTheatre.get(city);
        if (theatres == null) return theatreVsShows;

        for (Theatre theatre : theatres) {
            List<Show> givenMovieShows = new ArrayList<>();
            List<Show> shows = theatre.getShows();
            for (Show show : shows) {
                if (show.movie.getMovieId() == movie.getMovieId()) {
                    givenMovieShows.add(show);
                }
            }
            if (!givenMovieShows.isEmpty()) {
                theatreVsShows.put(theatre, givenMovieShows);
            }
        }
        return theatreVsShows;
    }
}

// -----------------------------
// Application entry point (BookMyShow) - highest level
// -----------------------------
public class BookMyShow {
    MovieController movieController;
    TheatreController theatreController;

    BookMyShow() {
        movieController = new MovieController();
        theatreController = new TheatreController();
    }

    public static void main(String args[]) {
        BookMyShow bookMyShow = new BookMyShow();
        bookMyShow.initialize();

        // user1
        bookMyShow.createBooking(City.Bangalore, "BAAHUBALI");
        // user2
        bookMyShow.createBooking(City.Bangalore, "BAAHUBALI");
    }

    private void createBooking(City userCity, String movieName) {
        // 1. search movie by my location
        List<Movie> movies = movieController.getMoviesByCity(userCity);

        // 2. select the movie which you want to see. i want to see Baahubali
        Movie interestedMovie = null;
        if (movies != null) {
            for (Movie movie : movies) {
                if ((movie.getMovieName()).equals(movieName)) {
                    interestedMovie = movie;
                }
            }
        }

        if (interestedMovie == null) {
            System.out.println("Movie not found in your city");
            return;
        }

        // 3. get all show of this movie in userCity location
        Map<Theatre, List<Show>> showsTheatreWise = theatreController.getAllShow(interestedMovie, userCity);
        if (showsTheatreWise.isEmpty()) {
            System.out.println("No shows available for this movie in your city");
            return;
        }

        // 4. select the particular show user is interested in (pick first theatre's first show as demo)
        Map.Entry<Theatre, List<Show>> entry = showsTheatreWise.entrySet().iterator().next();
        List<Show> runningShows = entry.getValue();
        Show interestedShow = runningShows.get(0);

        // 5. select the seat
        int seatNumber = 30;
        List<Integer> bookedSeats = interestedShow.getBookedSeatIds();
        if (!bookedSeats.contains(seatNumber)) {
            bookedSeats.add(seatNumber);
            // startPayment (omitted real payment; create booking object)
            Booking booking = new Booking();
            List<Seat> myBookedSeats = new ArrayList<>();
            for (Seat screenSeat : interestedShow.getScreen().getSeats()) {
                if (screenSeat.getSeatId() == seatNumber) {
                    myBookedSeats.add(screenSeat);
                }
            }
            booking.setBookedSeats(myBookedSeats);
            booking.setShow(interestedShow);
            // payment handling should occur here (omitted)
            System.out.println("BOOKING SUCCESSFUL");
        } else {
            System.out.println("seat already booked, try again");
            return;
        }
    }

    private void initialize() {
        // create movies
        createMovies();
        // create theater with screens, seats and shows
        createTheatre();
    }

    // creating 2 theatre
    private void createTheatre() {
        Movie avengerMovie = movieController.getMovieByName("AVENGERS");
        Movie baahubali = movieController.getMovieByName("BAAHUBALI");

        Theatre inoxTheatre = new Theatre();
        inoxTheatre.setTheatreId(1);
        inoxTheatre.setScreen(createScreen());
        inoxTheatre.setCity(City.Bangalore);
        List<Show> inoxShows = new ArrayList<>();
        Show inoxMorningShow = createShows(1, inoxTheatre.getScreen().get(0), avengerMovie, 8);
        Show inoxEveningShow = createShows(2, inoxTheatre.getScreen().get(0), baahubali, 16);
        inoxShows.add(inoxMorningShow);
        inoxShows.add(inoxEveningShow);
        inoxTheatre.setShows(inoxShows);

        Theatre pvrTheatre = new Theatre();
        pvrTheatre.setTheatreId(2);
        pvrTheatre.setScreen(createScreen());
        pvrTheatre.setCity(City.Delhi);
        List<Show> pvrShows = new ArrayList<>();
        Show pvrMorningShow = createShows(3, pvrTheatre.getScreen().get(0), avengerMovie, 13);
        Show pvrEveningShow = createShows(4, pvrTheatre.getScreen().get(0), baahubali, 20);
        pvrShows.add(pvrMorningShow);
        pvrShows.add(pvrEveningShow);
        pvrTheatre.setShows(pvrShows);

        theatreController.addTheatre(inoxTheatre, City.Bangalore);
        theatreController.addTheatre(pvrTheatre, City.Delhi);
    }

    private List<Screen> createScreen() {
        List<Screen> screens = new ArrayList<>();
        Screen screen1 = new Screen();
        screen1.setScreenId(1);
        screen1.setSeats(createSeats());
        screens.add(screen1);
        return screens;
    }

    private Show createShows(int showId, Screen screen, Movie movie, int showStartTime) {
        Show show = new Show();
        show.setShowId(showId);
        show.setScreen(screen);
        show.setMovie(movie);
        show.setShowStartTime(showStartTime); // 24 hrs time ex: 14 means 2pm and 8 means 8AM
        return show;
    }

    // creating 100 seats
    private List<Seat> createSeats() {
        List<Seat> seats = new ArrayList<>();
        // 1 to 40 : SILVER
        for (int i = 0; i < 40; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.SILVER);
            seats.add(seat);
        }
        // 41 to 70 : GOLD
        for (int i = 40; i < 70; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.GOLD);
            seats.add(seat);
        }
        // 71 to 99 : PLATINUM
        for (int i = 70; i < 100; i++) {
            Seat seat = new Seat();
            seat.setSeatId(i);
            seat.setSeatCategory(SeatCategory.PLATINUM);
            seats.add(seat);
        }
        return seats;
    }

    private void createMovies() {
        // create Movies1
        Movie avengers = new Movie();
        avengers.setMovieId(1);
        avengers.setMovieName("AVENGERS");
        avengers.setMovieDuration(128);

        // create Movies2
        Movie baahubali = new Movie();
        baahubali.setMovieId(2);
        baahubali.setMovieName("BAAHUBALI");
        baahubali.setMovieDuration(180);

        // add movies against the cities
        movieController.addMovie(avengers, City.Bangalore);
        movieController.addMovie(avengers, City.Delhi);
        movieController.addMovie(baahubali, City.Bangalore);
        movieController.addMovie(baahubali, City.Delhi);
    }
}

// How to handle Race Conditions?
