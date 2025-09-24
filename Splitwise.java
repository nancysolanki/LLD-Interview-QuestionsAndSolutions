/*
Manage Users and Groups.

Add Expenses belonging to a Group, created by a payer, and split among Group members.

Support different split types: EQUAL, EXACT, PERCENTAGE (Strategy pattern).

Maintain a BalanceSheet tracking what each user owes/gets from others.

Provide operations: createUser, createGroup, addExpense, settle payment between users, printBalances, printBalanceForUser, deleteExpense.

A SplitwiseService as the coordinator (Singleton). A SplitStrategyFactory chooses the strategy
*/
import java.util.*;

/*
 Bottom-up reordered Splitwise LLD
 Kept all classes, objects and method signatures. Minor internal impl details simplified/trimmed.
*/

// ------------------------
// 1) Core domain models
// ------------------------
class User {
    String userId;
    String userName;
    UserExpenseBalanceSheet userExpenseBalanceSheet;

    public User(String id, String userName){
        this.userId = id;
        this.userName = userName;
        this.userExpenseBalanceSheet = new UserExpenseBalanceSheet();
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public UserExpenseBalanceSheet getUserExpenseBalanceSheet() { return userExpenseBalanceSheet; }
}

class Balance {
    double amountOwe;
    double amountGetBack;

    public double getAmountOwe() { return amountOwe; }
    public void setAmountOwe(double amountOwe) { this.amountOwe = amountOwe; }
    public double getAmountGetBack() { return amountGetBack; }
    public void setAmountGetBack(double amountGetBack) { this.amountGetBack = amountGetBack; }
}

class Split {
    User user;
    double amountOwe;

    public Split(User user, double amountOwe){
        this.user = user;
        this.amountOwe = amountOwe;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public double getAmountOwe() { return amountOwe; }
    public void setAmountOwe(double amountOwe) { this.amountOwe = amountOwe; }
}

// ------------------------
// 2) Supporting structures
// ------------------------
class UserExpenseBalanceSheet {
    Map<String, Balance> userVsBalance;
    double totalYourExpense;
    double totalPayment;
    double totalYouOwe;
    double totalYouGetBack;

    public UserExpenseBalanceSheet(){
        userVsBalance = new HashMap<>();
        totalYourExpense = 0;
        totalPayment = 0;
        totalYouOwe = 0;
        totalYouGetBack = 0;
    }

    public Map<String, Balance> getUserVsBalance() { return userVsBalance; }
    public double getTotalYourExpense() { return totalYourExpense; }
    public void setTotalYourExpense(double totalYourExpense) { this.totalYourExpense = totalYourExpense; }
    public double getTotalYouOwe() { return totalYouOwe; }
    public void setTotalYouOwe(double totalYouOwe) { this.totalYouOwe = totalYouOwe; }
    public double getTotalYouGetBack() { return totalYouGetBack; }
    public void setTotalYouGetBack(double totalYouGetBack) { this.totalYouGetBack = totalYouGetBack; }
    public double getTotalPayment() { return totalPayment; }
    public void setTotalPayment(double totalPayment) { this.totalPayment = totalPayment; }
}

enum ExpenseSplitType {
    EQUAL,
    UNEQUAL,
    PERCENTAGE;
}

// ------------------------
// 3) Strategy pattern for splits
// ------------------------
interface ExpenseSplit {
    void validateSplitRequest(List<Split> splitList, double totalAmount);
}

class EqualExpenseSplit implements ExpenseSplit{
    @Override
    public void validateSplitRequest(List<Split> splitList, double totalAmount) {
        if(splitList == null || splitList.isEmpty()) throw new IllegalArgumentException("Splits required");
        double amountShouldBePresent = totalAmount / splitList.size();
        // allow small epsilon for floating point
        double eps = 0.0001;
        for(Split split: splitList) {
           if(Math.abs(split.getAmountOwe() - amountShouldBePresent) > eps) {
               throw new IllegalArgumentException("Equal split amounts do not match");
           }
        }
    }
}

class PercentageExpenseSplit implements ExpenseSplit {
    @Override
    public void validateSplitRequest(List<Split> splitList, double totalAmount) {
        if(splitList == null || splitList.isEmpty()) throw new IllegalArgumentException("Splits required");
        double sumPercent = 0;
        for(Split s: splitList){
            // here amountOwe represents percentage in [0..100]
            sumPercent += s.getAmountOwe();
        }
        double eps = 0.0001;
        if(Math.abs(sumPercent - 100.0) > eps) throw new IllegalArgumentException("Percentages must sum to 100");
        // actual per-user amount = (percent/100) * totalAmount - that calculation happens in controller
    }
}

class UnequalExpenseSplit implements ExpenseSplit {
    @Override
    public void validateSplitRequest(List<Split> splitList, double totalAmount) {
        if(splitList == null || splitList.isEmpty()) throw new IllegalArgumentException("Splits required");
        double sum = 0;
        for(Split s: splitList) sum += s.getAmountOwe();
        double eps = 0.0001;
        if(Math.abs(sum - totalAmount) > eps) throw new IllegalArgumentException("Unequal split amounts must sum to total");
    }
}

class SplitFactory {
    public static ExpenseSplit getSplitObject(ExpenseSplitType splitType) {
        switch (splitType) {
            case EQUAL: return new EqualExpenseSplit();
            case UNEQUAL: return new UnequalExpenseSplit();
            case PERCENTAGE: return new PercentageExpenseSplit();
            default: throw new IllegalArgumentException("Unknown split type");
        }
    }
}

// ------------------------
// 4) Expense and Group
// ------------------------
class Expense {
    String expenseId;
    String description;
    double expenseAmount;
    User paidByUser;
    ExpenseSplitType splitType;
    List<Split> splitDetails = new ArrayList<>();

    public Expense(String expenseId, double expenseAmount, String description,
                   User paidByUser, ExpenseSplitType splitType, List<Split> splitDetails) {
        this.expenseId = expenseId;
        this.expenseAmount = expenseAmount;
        this.description = description;
        this.paidByUser = paidByUser;
        this.splitType = splitType;
        if(splitDetails != null) this.splitDetails.addAll(splitDetails);
    }

    public String getExpenseId(){ return expenseId; }
    public double getExpenseAmount(){ return expenseAmount; }
    public User getPaidByUser(){ return paidByUser; }
    public List<Split> getSplitDetails(){ return splitDetails; }
}

class Group {
    String groupId;
    String groupName;
    List<User> groupMembers;
    List<Expense> expenseList;
    ExpenseController expenseController;

    Group(){
        groupMembers = new ArrayList<>();
        expenseList = new ArrayList<>();
        expenseController = new ExpenseController();
    }

    //add member to group
    public void addMember(User member){ groupMembers.add(member); }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Expense createExpense(String expenseId, String description, double expenseAmount,
                                 List<Split> splitDetails, ExpenseSplitType splitType, User paidByUser) {
        Expense expense = expenseController.createExpense(expenseId, description, expenseAmount, splitDetails, splitType, paidByUser);
        expenseList.add(expense);
        return expense;
    }
}

// ------------------------
// 5) Controllers / Managers
// ------------------------
class UserController {
    List<User> userList;
    public UserController(){ userList = new ArrayList<>(); }
    public void addUser(User user) { userList.add(user); }
    public User getUser(String userID) {
        for (User user : userList) if (user.getUserId().equals(userID)) return user;
        return null;
    }
    public List<User> getAllUsers(){ return userList; }
}

class GroupController {
    List<Group> groupList;
    public GroupController(){ groupList = new ArrayList<>(); }
    public void createNewGroup(String groupId, String groupName, User createdByUser) {
        Group group = new Group();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.addMember(createdByUser);
        groupList.add(group);
    }
    public Group getGroup(String groupId){
        for(Group g: groupList) if(g.getGroupId().equals(groupId)) return g;
        return null;
    }
}

// BalanceSheetController made singleton so ExpenseController can access it easily
class BalanceSheetController {
    private static BalanceSheetController INSTANCE = new BalanceSheetController();
    private BalanceSheetController() {}
    public static BalanceSheetController getInstance(){ return INSTANCE; }

    public void updateUserExpenseBalanceSheet(User expensePaidBy, List<Split> splits, double totalExpenseAmount){
        UserExpenseBalanceSheet paidByUserExpenseSheet = expensePaidBy.getUserExpenseBalanceSheet();
        paidByUserExpenseSheet.setTotalPayment(paidByUserExpenseSheet.getTotalPayment() + totalExpenseAmount);

        for(Split split : splits) {
            User userOwe = split.getUser();
            UserExpenseBalanceSheet oweUserExpenseSheet = userOwe.getUserExpenseBalanceSheet();
            double oweAmount = split.getAmountOwe();

            if(expensePaidBy.getUserId().equals(userOwe.getUserId())){
                paidByUserExpenseSheet.setTotalYourExpense(paidByUserExpenseSheet.getTotalYourExpense()+oweAmount);
            }
            else {
                paidByUserExpenseSheet.setTotalYouGetBack(paidByUserExpenseSheet.getTotalYouGetBack() + oweAmount);

                Balance userOweBalance = paidByUserExpenseSheet.getUserVsBalance().getOrDefault(userOwe.getUserId(), new Balance());
                paidByUserExpenseSheet.getUserVsBalance().putIfAbsent(userOwe.getUserId(), userOweBalance);
                userOweBalance.setAmountGetBack(userOweBalance.getAmountGetBack() + oweAmount);

                oweUserExpenseSheet.setTotalYouOwe(oweUserExpenseSheet.getTotalYouOwe() + oweAmount);
                oweUserExpenseSheet.setTotalYourExpense(oweUserExpenseSheet.getTotalYourExpense() + oweAmount);

                Balance userPaidBalance = oweUserExpenseSheet.getUserVsBalance().getOrDefault(expensePaidBy.getUserId(), new Balance());
                oweUserExpenseSheet.getUserVsBalance().putIfAbsent(expensePaidBy.getUserId(), userPaidBalance);
                userPaidBalance.setAmountOwe(userPaidBalance.getAmountOwe() + oweAmount);
            }
        }
    }

    public void showBalanceSheetOfUser(User user){
        System.out.println("---------------------------------------");
        System.out.println("Balance sheet of user : " + user.getUserId());
        UserExpenseBalanceSheet s = user.getUserExpenseBalanceSheet();
        System.out.println("TotalYourExpense: " + s.getTotalYourExpense());
        System.out.println("TotalGetBack: " + s.getTotalYouGetBack());
        System.out.println("TotalYourOwe: " + s.getTotalYouOwe());
        System.out.println("TotalPaymnetMade: " + s.getTotalPayment());
        for(Map.Entry<String, Balance> entry : s.getUserVsBalance().entrySet()){
            String userID = entry.getKey();
            Balance balance = entry.getValue();
            System.out.println("userID:" + userID + " YouGetBack:" + balance.getAmountGetBack() + " YouOwe:" + balance.getAmountOwe());
        }
        System.out.println("---------------------------------------");
    }
}

class ExpenseController {
    public Expense createExpense(String expenseId, String description, double expenseAmount,
                                 List<Split> splitDetails, ExpenseSplitType splitType, User paidByUser) {
        // Validate
        ExpenseSplit validator = SplitFactory.getSplitObject(splitType);

        // For percentage type, caller is expected to provide percent values in split.amountOwe
        validator.validateSplitRequest(splitDetails, expenseAmount);

        // If percentage, convert splitDetails percentages to actual amounts
        if(splitType == ExpenseSplitType.PERCENTAGE){
            for(Split s: splitDetails){
                double percent = s.getAmountOwe();
                s.setAmountOwe((percent/100.0) * expenseAmount);
            }
        }

        Expense expense = new Expense(expenseId, expenseAmount, description, paidByUser, splitType, splitDetails);

        // Update balances via BalanceSheetController singleton
        BalanceSheetController.getInstance().updateUserExpenseBalanceSheet(paidByUser, splitDetails, expenseAmount);

        return expense;
    }
}

// ------------------------
// 6) Core Service (application coordinator)
// ------------------------
class Splitwise {
    UserController userController;
    GroupController groupController;
    BalanceSheetController balanceSheetController;

    Splitwise(){
        userController = new UserController();
        groupController = new GroupController();
        balanceSheetController = BalanceSheetController.getInstance();
    }

    public void demo(){
        setupUserAndGroup();

        // add members to the group
        Group group = groupController.getGroup("G1001");
        group.addMember(userController.getUser("U2001"));
        group.addMember(userController.getUser("U3001"));

        // create an expense inside a group (equal split)
        List<Split> splits = new ArrayList<>();
        splits.add(new Split(userController.getUser("U1001"), 300));
        splits.add(new Split(userController.getUser("U2001"), 300));
        splits.add(new Split(userController.getUser("U3001"), 300));
        group.createExpense("Exp1001", "Breakfast", 900, splits, ExpenseSplitType.EQUAL, userController.getUser("U1001"));

        // create unequal expense
        List<Split> splits2 = new ArrayList<>();
        splits2.add(new Split(userController.getUser("U1001"), 400));
        splits2.add(new Split(userController.getUser("U2001"), 100));
        group.createExpense("Exp1002", "Lunch", 500, splits2, ExpenseSplitType.UNEQUAL, userController.getUser("U2001"));

        // create percentage expense (percent values provided)
        List<Split> percentSplits = new ArrayList<>();
        percentSplits.add(new Split(userController.getUser("U1001"), 50)); // 50%
        percentSplits.add(new Split(userController.getUser("U2001"), 50)); // 50%
        group.createExpense("Exp1003", "Taxi", 200, percentSplits, ExpenseSplitType.PERCENTAGE, userController.getUser("U1001"));

        // Show balances
        for(User user : userController.getAllUsers()) {
            balanceSheetController.showBalanceSheetOfUser(user);
        }
    }

    public void setupUserAndGroup(){
        addUsersToSplitwiseApp();
        User user1 = userController.getUser("U1001");
        groupController.createNewGroup("G1001", "Outing with Friends", user1);
    }

    private void addUsersToSplitwiseApp(){
        User user1 = new User("U1001", "User1");
        User user2 = new User ("U2001", "User2");
        User user3 = new User ("U3001", "User3");
        userController.addUser(user1);
        userController.addUser(user2);
        userController.addUser(user3);
    }
}

// ------------------------
// 7) Entry point
// ------------------------
public class Main {
    public static void main(String args[]){
        Splitwise splitwise = new Splitwise();
        splitwise.demo();
    }
}
