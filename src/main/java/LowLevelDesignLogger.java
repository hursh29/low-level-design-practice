import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

enum LogLevel {
    DEBUG, INFO, WARN, ERROR
}

class LogEvent {
    final String msg;
    final LogLevel level;
    final ZonedDateTime timestamp;

    public LogEvent(String msg, LogLevel level, ZonedDateTime timestamp) {
        this.msg = msg;
        this.level = level;
        this.timestamp = timestamp;
    }
}

interface Filter {
    boolean accept(LogEvent event);
}

class LevelFilter implements Filter {

    final List<LogLevel> levelsToBeFiltered = new ArrayList<>();

    public LevelFilter addLevelsToBeFiltered(final LogLevel inputLevel) {
        levelsToBeFiltered.add(inputLevel);
        return this;
    }

    @Override
    public boolean accept(LogEvent event) {
        return levelsToBeFiltered.contains(event.level);
    }
}

interface Formatter {
    String format(LogEvent event);
}

// this is chat gpt generated formatter
class PrettyFormatter implements Formatter {
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z");

    @Override
    public String format(LogEvent event) {
        return String.format("[%s] [%-5s] - %s",
            event.timestamp.format(TIME_FORMATTER),
            event.level,
            event.msg);
    }
}

class SimpleTextFormatter implements Formatter {

    @Override
    public String format(LogEvent event) {
        return String.format("%s %s %s",
            event.timestamp.format(DateTimeFormatter.ISO_DATE_TIME),
            event.level,
            event.msg
        );
    }
}

interface Appender {
    void append(LogEvent event);
    void close();
}

class ConsoleAppender implements Appender {

    private Formatter formatter;
    private Filter filter;

    public ConsoleAppender(Formatter formatter, Filter filter) {
        this.formatter = formatter;
        this.filter = filter;
    }

    @Override
    public void append(LogEvent event) {
        if (filter.accept(event)) {
            return;
        }
        final var formattedResponse = formatter.format(event);
        System.out.println(formattedResponse);
    }

    @Override
    public void close() {
        System.out.println("Nothing to do");
    }
}

class FileAppender implements Appender {

    private final File file;
    private final Formatter formatter;
    private final Filter filter;
    private FileWriter fileWriter;

    public FileAppender(Formatter formatter, Filter filter, String filePath) {
        this.formatter = formatter;
        this.filter = filter;
        this.file = new File(filePath);

        try {
            fileWriter = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void append(LogEvent event) {
        if (filter.accept(event)) {
            return;
        }
        final var formattedResponse = formatter.format(event);
        try {
            fileWriter.write(formattedResponse + System.lineSeparator());
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(formattedResponse);
    }

    @Override
    public void close() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Logger {
    private static Logger loggerInstance;
    private final List<Appender> appenders = new ArrayList<>();

    private Logger() {

    }

    public void addAppenders(final Appender appender) {
        appenders.add(appender);
    }

    public static Logger getLoggerInstance() {
        if (loggerInstance == null) {
            loggerInstance = new Logger();
        }

        return loggerInstance;
    }

    public void info(final String msg) {
        final var logEvent = new LogEvent(msg, LogLevel.INFO, ZonedDateTime.now());
        appenders.forEach(appender -> appender.append(logEvent));
    }

    public void warn(final String msg) {
        final var logEvent = new LogEvent(msg, LogLevel.WARN, ZonedDateTime.now());
        appenders.forEach(appender -> appender.append(logEvent));
    }

    public void error(final String msg) {
        final var logEvent = new LogEvent(msg, LogLevel.ERROR, ZonedDateTime.now());
        appenders.forEach(appender -> appender.append(logEvent));
    }

    public void debug(final String msg) {
        final var logEvent = new LogEvent(msg, LogLevel.DEBUG, ZonedDateTime.now());
        appenders.forEach(appender -> appender.append(logEvent));
    }

    public void close() {
        appenders.forEach(Appender::close);
    }
}

public class LowLevelDesignLogger {
    public static void main(String[] args) {
        final Logger logger = Logger.getLoggerInstance();

        final Formatter formatter = new SimpleTextFormatter();
        final LevelFilter filter = new LevelFilter().addLevelsToBeFiltered(LogLevel.DEBUG);

        logger.addAppenders(new ConsoleAppender(formatter, filter));
        logger.addAppenders(new FileAppender(new PrettyFormatter(), filter, "systemLogs.log"));

        for (int i = 0 ; i < 100 ; i += 1) {
            logger.info("Hello World!" + i);
            logger.debug("Trobuleshooting" + i);
        }
        logger.error("Something went wrong");
    }
}
