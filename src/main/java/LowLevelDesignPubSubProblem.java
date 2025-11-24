
// Message
// Topic (Queues)
// Publisher
// Subscriber
// Broker

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

class Message {
    @Override
    public String toString() {
        return "Message{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            "}  ";
    }

    private final String key;
    private final String value;
    private final ZonedDateTime createdAt;

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    private Long offset;

    public Message(String key, String value) {
        this.key = key;
        this.value = value;
        this.createdAt = ZonedDateTime.now();
//        this.offset = offset;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getOffset() {
        return offset;
    }
}

enum PubSubUserType {
    PUBLISHER, SUBSCRIBER, BROKER_MANAGER
}

interface PubSubUser {
    Long getId();

    PubSubUserType getType();
}

class Publisher implements PubSubUser {
    private final AtomicLong ID_GENERATOR = new AtomicLong(0L);
    private final String name;
    private final Long id;

    Publisher(String name) {
        this.name = name;
        id = ID_GENERATOR.getAndIncrement();
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public PubSubUserType getType() {
        return PubSubUserType.PUBLISHER;
    }

    public void publish(Topic t, Message msg) {
        t.publish(msg);
    }
}

class Subscriber implements PubSubUser {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0L);

    public String getName() {
        return name;
    }

    private final String name;
    private final Long id;

    Subscriber(String name) {
        this.name = name;
        id = ID_GENERATOR.incrementAndGet();
    }
    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public PubSubUserType getType() {
        return PubSubUserType.SUBSCRIBER;
    }

    public void consume(final Message msg) {
        System.out.print(this.name + " am consuming this message   -> ");
        System.out.print(msg);
        System.out.println(msg.getOffset() +  " consumed ---------------");
    }
}

class Topic {
    private final AtomicLong ID_GENERATOR = new AtomicLong(0L);
    private final List<Message> messages;

    public Long getId() {
        return id;
    }

    private final Long id;
    private final List<Subscriber> subscribers;
    private final Map<Subscriber, Long> subscriberToLastReadOffset;

    public Topic() {
        id = ID_GENERATOR.getAndIncrement();
        messages = new ArrayList<>();
        subscribers = new ArrayList<>();
        subscriberToLastReadOffset = new HashMap<>();
    }

    public boolean addSubscriber(final Subscriber newSubscriber) {
        final var alreadyExists = subscribers.stream()
            .filter(subs -> newSubscriber.getId().equals(subs.getId()))
            .findAny()
            .isPresent();

        if (alreadyExists) {
            System.err.println("Subscriber already exists" + newSubscriber.getName());
            return false;
        }

        System.out.println("Successfully added subscriber - "  + newSubscriber.getId());

        final var lastReadOffSet = Long.valueOf(messages.size());

        subscriberToLastReadOffset.put(newSubscriber, lastReadOffSet - 1);
        subscribers.add(newSubscriber);
        return true;
    }

    public boolean removeSubscriber(final Long toBeRemovedId) {
        final var alreadyExists = subscribers.stream()
            .filter(subs -> toBeRemovedId.equals(subs.getId()))
            .findAny();

        if (alreadyExists.isEmpty()) {
            System.err.println("Subsriber doest not exist exists");
            return false;
        }

        subscribers.remove(alreadyExists.get());
        subscriberToLastReadOffset.remove(alreadyExists.get());
        return true;
    }

    public void publish(final Message msg) {
        msg.setOffset(Long.valueOf(messages.size()));
        messages.add(msg);

        this.subscribers.forEach(subscriber -> {
            try {
                subscriber.consume(msg);
                subscriberToLastReadOffset.put(subscriber, msg.getOffset());
            } catch(Exception ex) {
                System.err.println("Subscriber is down");
            }
        });
    }
}

class Broker {
    private final List<Topic> topics;

    Broker() {
        topics = new ArrayList<>();
    }

    private Optional<Topic> findTopicById(final Long topicId) {
        return topics.stream()
            .filter(topic -> topic.getId().equals(topicId))
            .findFirst();
    }

    public boolean addSubscriber(final Long topicId, final Subscriber subs) {
        return findTopicById(topicId)
            .map(topic -> topic.addSubscriber(subs))
            .orElse(false);
    }

    public boolean removeSubscriber(final Long topicId, final Long subsId) {
        return findTopicById(topicId)
            .map(topic -> topic.removeSubscriber(subsId))
            .orElse(false);
    }

    public void publish(final Long topicId, final Message msg) {
        findTopicById(topicId).ifPresent(topic -> topic.publish(msg));
    }

    public Topic onboardNewTopic() {
        final var newTopic = new Topic();

        topics.add(newTopic);

        return newTopic;
    }
}

public class LowLevelDesignPubSubProblem {

    public static void main(String[] args) {
        final var b1 = new Broker();

        final var t1 = b1.onboardNewTopic(); // Topic - 0
        final var t2 = b1.onboardNewTopic(); // Topic - 1

        final var s1 = new Subscriber("hursh");
        final var s2 = new Subscriber("vasudha");
        final var s3 = new Subscriber("raman");
        final var s4 = new Subscriber("manvi");

        final var p1 = new Publisher("aastha");

        t1.addSubscriber(s1);
        t1.addSubscriber(s2);
        t1.addSubscriber(s3);
        t1.addSubscriber(s4);

        p1.publish(t1, new Message("1", "Hursh went to Samay Raina's show"));
        p1.publish(t1, new Message("2", "Hursh went to Samay Raina's show today"));
        p1.publish(t1, new Message("3", "Hursh went to Samay Raina's show today at 8 PM"));

        System.out.println("-----------------");

        t1.removeSubscriber(s2.getId());

        p1.publish(t1, new Message("1", "Vasudha went to Sonu Nigam's show"));
        p1.publish(t1, new Message("2", "Vasudha went to Sonu Nigam's show today"));
        p1.publish(t1, new Message("3", "Vasudha went to Sonu Nigam's show today at 8 PM"));
    }
}
