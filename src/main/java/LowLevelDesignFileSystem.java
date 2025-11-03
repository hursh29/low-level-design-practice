import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

enum FileSystemEntityType {
    DIRECTORY, FILE
}

abstract class FileSystemEntity {
    protected final String name;
    protected final FileSystemEntityType type;

    FileSystemEntity(final String name, final FileSystemEntityType type) {
        this.name = name;
        this.type = type;
    }
}

class File extends FileSystemEntity {
    final long size;

    public File(
        final String name, final FileSystemEntityType type,
        final long size) {
        super(name, type);
        this.size = size;
    }

    @Override
    public String toString() {
        return "name='" + name + '\'' +
            ", type=" + type +
            ", size=" + size;
    }
}

class Directory extends FileSystemEntity {
    private final Map<String, FileSystemEntity> childrenEntities;

    public Directory(final String name, final FileSystemEntityType type) {
        super(name, type);
        this.childrenEntities = new HashMap<>();
    }

    public List<FileSystemEntity> getAllContent() {
        return childrenEntities.values().stream()
            .collect(Collectors.toList());
    }

    public Optional<FileSystemEntity> getEntity(final String fileName) {
        return childrenEntities.keySet().stream()
            .filter(fileName::equalsIgnoreCase)
            .map(childrenEntities::get)
            .findFirst();
    }

    public void addFile(final File newFile) {
        childrenEntities.put(newFile.name, newFile);
    }

    public void addDirectory(final Directory newDir) {
        childrenEntities.put(newDir.name, newDir);
    }

    public void deleteEntity(final FileSystemEntity fileSystemEntity) {
        childrenEntities.remove(fileSystemEntity.name);
    }

    @Override
    public String toString() {
        return "Directory  " +
            "name='" + name + '\'' +
            ", type=" + type;
    }
}

class FileSystemClient {

    private final FileSystemEntity root;

    public FileSystemClient() {
        this.root = new Directory("", FileSystemEntityType.DIRECTORY);
    }

    private AbstractMap.SimpleEntry<FileSystemEntity, FileSystemEntity> navigateToPath(final String path) {
        final List<String> splitEntities = Arrays.stream(path.split("/"))
            .filter(entity -> !entity.isEmpty())
            .collect(Collectors.toList());

        Directory currentEntity = (Directory) this.root;

        if (splitEntities.isEmpty()) {
            return new AbstractMap.SimpleEntry<>(currentEntity, currentEntity);
        }

        int i = 0;
        for (i = 0 ; i < splitEntities.size() - 1; i += 1) {
            final var currentName = splitEntities.get(i);
            final var mayBeDirectory = currentEntity.getEntity(currentName);
            if (mayBeDirectory.isPresent() && mayBeDirectory.get().type.equals(FileSystemEntityType.DIRECTORY)) {
                currentEntity = (Directory) mayBeDirectory.get();
            } else {
                throw new IllegalStateException("Path not found at " + currentName);
            }
        }
        final var lastEntityName = splitEntities.get(i);

        return new AbstractMap.SimpleEntry<>(
            currentEntity.getEntity(lastEntityName).get(),
            currentEntity);
    }

    public boolean createFile(final String name, final String path, final long size) {
        final var navigatedLocation = (Directory) navigateToPath(path).getKey();
        final var newFile = new File(name, FileSystemEntityType.FILE, size);

        navigatedLocation.addFile(newFile);
        return true;
    }

    public boolean createFolder(final String name, final String path) {
        final var navigatedLocation = (Directory) navigateToPath(path).getKey();
        final var newDirectory = new Directory(name, FileSystemEntityType.DIRECTORY);

        navigatedLocation.addDirectory(newDirectory);
        return true;
    }

    public void listContent(final String path) {
        final var navigatedLocation = navigateToPath(path).getKey();

        ((Directory) navigatedLocation).getAllContent()
            .forEach(System.out::println);
    }

    private long aggregateSize(final FileSystemEntity entity) {
        AtomicLong initialSize = new AtomicLong(0);

        if (entity.type.equals(FileSystemEntityType.DIRECTORY)) {
            ((Directory) entity).getAllContent()
                .forEach((childEntity) -> {
                    initialSize.addAndGet(aggregateSize(childEntity));
                });
        } else {
            initialSize.set(((File) entity).size);
        }

        return initialSize.get();
    }

    public long getSize(final String path) {
        final var navigatedLocation = navigateToPath(path).getKey();

        return aggregateSize(navigatedLocation);
    }

    public void deleteEntity(final String path) {
        final var navigatedPath = navigateToPath(path);
        final var currentEntity = navigatedPath.getKey();
        final var parent = navigatedPath.getValue();

        ((Directory) parent).deleteEntity(currentEntity);
    }
}

public class LowLevelDesignFileSystem {

    public static void main(String[] args) {
        final var client = new FileSystemClient();
        client.createFolder("grandparent", "/");
        client.createFile("theOffice.mp4", "/", 200L);
        client.createFile("friends.txt", "/", 128L);

        client.createFolder("parent", "/grandparent");
        client.createFile("angelinaJolie.mp4", "/grandparent", 200L);
        client.createFile("bradPitt.txt", "/grandparent", 128L);

        client.listContent("/");
        client.listContent("/grandparent");

        System.out.println("Printing size = " + client.getSize("/"));
        System.out.println("Printing size = " + client.getSize("/grandparent"));

        client.deleteEntity("/grandparent/angelinaJolie.mp4");
        System.out.println("Printing size = " + client.getSize("/grandparent"));

        client.deleteEntity("/grandparent");
        System.out.println("Printing size = " + client.getSize("/"));

        client.deleteEntity("theOffice.mp4");
        client.deleteEntity("friends.txt");

        System.out.println("Printing size = " + client.getSize("/"));
    }
}

