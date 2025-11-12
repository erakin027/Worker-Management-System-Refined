public abstract class Work {
    private String workName;
    private int time;
    private double price;

    public Work(String workName, int time, double price){
        this.workName = workName;
        this.time = time;
        this.price = price;
    }

    public String getWorkName() {
        return workName;
    }

    public int getTimeRequired() {
        return time;
    }

    public Double getPrice(){
        return price;
    }
    //public abstract void showDetails();
}


class HouseCleaning extends Work{
    public HouseCleaning(String workName, int time, double price){
        super(workName, time, price);
    }
};

class WindowCleaning extends HouseCleaning{
    
    public WindowCleaning(){
        super("Window Cleaning", 80, 600);
    }
};
class Mopping extends HouseCleaning{
    public Mopping(){
        super("Mopping", 40,300);
    }
};
class Sweeping extends HouseCleaning{
    public Sweeping(){
        super("Sweeping", 30, 200);
    }
};
class FanCleaning extends HouseCleaning{
    public FanCleaning(){
        super("Fan Cleaning", 40, 400);
    }
};
class BathroomCleaning extends HouseCleaning{
    public BathroomCleaning(){
        super("Bathroom Cleaning", 60,500);
    }
};


class GardenCleaning extends Work{
    public GardenCleaning(String workName, int time, double price){
        super(workName, time, price);
    }
};

class Mowing extends GardenCleaning{
    public Mowing(){
        super("Mowing", 80,700);
    }
};
class Pruning  extends GardenCleaning{
    public Pruning(){
        super("Pruning", 120,900);
    }
};


class Laundry extends Work{
    public Laundry(String workName, int time, double price){
        super(workName, time, price);
    }
};

class Washing extends Laundry{
    public Washing(){
        super("Washing", 40,300);
    }
};
class Drying extends Laundry{
    public Drying(){
        super("Drying", 30,200);
    }
};

class Ironing extends Laundry{
    public Ironing(){
        super("Ironing", 40, 200);
    }
};