/*
Firstly we will go through the requirements:-

Any non-member (guest) can search and view questions. However, they must become a member to add or upvote a question.
Members should be able to post new questions.
Members should be able to add an answer to an open question.
Members can add comments to any question or answer.
A member can upvote a question, answer, or comment.
Members can flag a question, answer, or comment, for serious problems or moderator attention.
Members will earn badges for being helpful.
Members can vote to close a question; 
Moderators : Can close or reopen questions, override community votes, and undelete deleted posts.

Acts as the administrator or superuser in the system.Moderators can close or reopen any question.
Members can add tags to their questions. A tag is a word or phrase describing the question's topic.
Members can vote to delete extremely off-topic or very low-quality questions.
*/

/*The Factory Pattern is used to create different types of entities (Question, Answer, Comment) without exposing the instantiation logic. This pattern provides flexibility when creating new objects, 
  and we can easily extend it by adding more entity types.*/

public interface EntityFactory {
    Entity createEntity(EntityType type);
}

public enum EntityType {
    QUESTION, ANSWER, COMMENT;
}

public class EntityFactoryImpl implements EntityFactory {

    @Override
    public Entity createEntity(EntityType type) {
        switch (type) {
            case QUESTION:
                return new Question();
            case ANSWER:
                return new Answer();
            case COMMENT:
                return new Comment();
            default:
                throw new IllegalArgumentException("Unknown entity type");
        }
    }
}

//Enums for Account Status and Badge Types

public enum AccountStatus {
    ACTIVE, CLOSED, BLOCKED; //status of User Account
}

public enum BadgeType {
    BRONZE, SILVER, GOLD, PLATINUM;
}

// we will now crete Interface with different resposibilties :

//Each interface defines a set of behaviors (methods) that certain classes (like Member, Moderator) will be implemented, adhering to the Interface Segregation Principle (ISP). 
//This keeps the interfaces focused on specific functionalities.
interface Searchable {
    List<Question> search(String searchString);
}

interface Postable {
    void postQuestion(Question question);
    void addAnswer(Question question, Answer answer);
    void addComment(Entity entity, Comment comment);
}

interface Votable {
    void vote(Entity entity, VoteType voteType);
}

interface Taggable {
    void addTag(Question question, Tag tag);
}

interface Flaggable {
    void flagEntity(Entity entity);
}

interface BadgeAwardable {
    List<Badge> getBadges();
}

interface Moderatable {
    void deleteQuestion(Question question);
    void undeleteQuestion(Question question);
}


// Now instead of creating seperate classes for Ques , ans , comments  common functiionalities 
//An abstract class that encapsulates common properties and behaviors for Question, Answer, and Comment. It manages votes, comments, and flags. This promotes code reuse

abstract class Entity {
    protected int entityId;
    protected Member createdBy;
    protected Date createdDate;
    protected Map<VoteType, Integer> votes = new HashMap<>();
    protected List<Comment> comments = new ArrayList<>();
    protected List<Member> flaggedMembers = new ArrayList<>();

    public void flagEntity(Member member) {
        flaggedMembers.add(member);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void voteEntity(VoteType voteType) {
        votes.put(voteType, votes.getOrDefault(voteType, 0) + 1);
    }
}
/*Comment, Answer, and Question extend the Entity class, inheriting its properties and methods. Each class has its specific attributes and methods 
(e.g., acceptAnswer in Answer, adding answers/tags in Question).*/
class Comment extends Entity {
    private String message;

    public Comment(String message) {
        this.message = message;
    }
}

class Answer extends Entity {
    private String answerText;
    private boolean isAccepted;

    public Answer(String answerText) {
        this.answerText = answerText;
        this.isAccepted = false;
    }

    public void acceptAnswer() {
        this.isAccepted = true;
    }
}

class Question extends Entity {
    private String title;
    private String description;
    private List<Answer> answers = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();

    public Question(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void editQuestion(String title, String description) {
        this.title = title;
        this.description = description;
    }
}

/*User: Implements the Searchable interface, allowing users (including guests) to search for questions.
Member: Extends User and implements several interfaces (Postable, Votable, etc.), encapsulating functionalities related to posting, voting, tagging, flagging, and badge management.
This promotes code organization and adherence to the Single Responsibility Principle.*/

class User implements Searchable {
    private int guestId;
    private SearchService searchService;

    public User(SearchService searchService) {
        this.searchService = searchService;
    }

    public List<Question> search(String searchString) {
        return searchService.searchQuestions(searchString);
    }
}

class Member extends User implements Postable, Votable, Taggable, Flaggable, BadgeAwardable {
    private Account account;
    private List<Badge> badgesEarned = new ArrayList<>();

    public Member(SearchService searchService, Account account) {
        super(searchService);
        this.account = account;
    }

    public void postQuestion(Question question) {
        // Posting logic
    }

    public void addAnswer(Question question, Answer answer) {
        question.addAnswer(answer);
    }

    public void addComment(Entity entity, Comment comment) {
        entity.addComment(comment);
    }

    public void vote(Entity entity, VoteType voteType) {
        entity.voteEntity(voteType);
    }

    public void addTag(Question question, Tag tag) {
        question.addTag(tag);
    }

    public List<Badge> getBadges() {
        return badgesEarned;
    }

    public void flagEntity(Entity entity) {
        entity.flagEntity(this);
    }
}

/*Moderator: Extends Member and implements the Moderatable interface, providing additional functionality specific to moderators, like deleting and undeleting questions.*/
class Moderator extends Member implements Moderatable {
    public Moderator(SearchService searchService, Account account) {
        super(searchService, account);
    }

    public void deleteQuestion(Question question) {
        // Deleting logic
    }

    public void undeleteQuestion(Question question) {
        // Undeleting logic
    }
}

/*SearchService: Contains the logic for searching questions based on a string. This decouples the search functionality from the User and Member classes, adhering to the Separation of Concerns principle.
VoteService: Handles voting logic, which can be expanded independently of the entities.*/

class SearchService {
    public List<Question> searchQuestions(String searchString) {
        // Searching logic includes search via question or tag or user.
        return new ArrayList<>();
    }
}

class VoteService {
    public void upvote(Entity entity) {
        // Upvote logic
    }

    public void downvote(Entity entity) {
        // Downvote logic
    }
}

/*Account: Encapsulates user account information and state.
Badge: Represents a badge earned by a member, with attributes for the type and description.*/
class Account {
    private String name;
    private String userName;
    private String password;
    private String email;
    private AccountStatus accountStatus;
    private int reputation;
    // Getters and setters
}

class Badge {
    private BadgeType badgeType;
    private String description;

    public Badge(BadgeType badgeType, String description) {
        this.badgeType = badgeType;
        this.description = description;
    }
}
/*Vote: A separate class to encapsulate the concept of a vote we can in fture  implement the strategy pattern.*/
class Vote {
    private VoteType voteType;

    public Vote(VoteType voteType) {
        this.voteType = voteType;
    }

    public void applyVote(Entity entity) {
        entity.voteEntity(voteType);
    }
}
