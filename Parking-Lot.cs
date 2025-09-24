/* Requirements (inferred — common interview set):

Support multiple vehicle types: Motorcycle, Car, Bus.

Multiple parking levels/floors; each floor has spots of different sizes (Compact, Regular, Large).

Find nearest available spot for incoming vehicle and issue a ticket with entry time.

Release spot on exit, calculate fee based on duration and vehicle type.

Allow Checking  current occupancy and available spots for parking.
 */

/*
Design flow (bottom → top):

Start with fundamental enums and simple model classes: Vehicle, ParkingSpot, Ticket.

Add utility components: IParkingFeeCalculator (Strategy pattern) to allow flexible fee policies.

Add factories where helpful (simple VehicleFactory) to map types.

Core orchestrator is ParkingLot implemented as a Singleton to represent the entire system; it manages ParkingFloors and spot allocation.

ParkingManager exposes primary operations used by callers (park/unpark, status). This separates orchestration from storage.
*/

/// <summary>
/// Enums: VehicleType — types of vehicles supported.
/// </summary>
public enum VehicleType
{
    Motorcycle,
    Car,
    Bus
}

/// <summary>
/// Enums: SpotSize — sizes of parking spots.
/// </summary>
public enum SpotSize
{
    Compact,    // for Motorcycle, some Cars
    Regular,    // for Cars
    Large       // for Bus, large vehicles
}

/// <summary>
/// Represents a Vehicle with minimal properties: Id, Type, LicensePlate.
/// </summary>
public class Vehicle
{
    public string Id { get; set; }               // GUID or string id
    public VehicleType Type { get; set; }
    public string LicensePlate { get; set; }

    public Vehicle(VehicleType type, string licensePlate)
    {
        Id = Guid.NewGuid().ToString();
        Type = type;
        LicensePlate = licensePlate;
    }
}
/// <summary>
/// VehicleFactory: a tiny factory that may be extended to create different vehicles.
/// THIS IS JUST A wRAPPER ON " new "to create objects , actuall factory pattern will have clasees of Bike Moter etc objects like this 

/*public static class VehicleFactory
{
    public static IVehicle CreateVehicle(VehicleType type, string license)
    {
        return type switch
        {
            VehicleType.Car => new Car(license),
            VehicleType.Bus => new Bus(license),
            VehicleType.Motorcycle => new Motorcycle(license),
            _ => throw new ArgumentException("Invalid type")
        };
    }
}*/
/// </summary>
public static class VehicleFactory
{
    public static Vehicle CreateVehicle(VehicleType type, string license)
    {
        return new Vehicle(type, license);
    }
}

/// <summary>
/// ParkingSpot: Models a single spot — number, size, whether occupied and reference to current ticket.
/// </summary>
public class ParkingSpot
{
    public string Id { get; set; }  // unique spot id (e.g., "F1-S12")
    public SpotSize Size { get; set; }
    public bool IsOccupied { get; set; }
    public string CurrentTicketId { get; set; }  // null when empty

    public ParkingSpot(string id, SpotSize size)
    {
        Id = id;
        Size = size;
        IsOccupied = false;
        CurrentTicketId = null; // null when empty
    }

    public bool CanFitVehicle(Vehicle vehicle)
    {
        // Basic mapping: Motorcycle -> Compact, Car -> Regular/Compact, Bus -> Large
        switch (vehicle.Type)
        {
            case VehicleType.Motorcycle:
                return Size == SpotSize.Compact;
            case VehicleType.Car:
                return Size == SpotSize.Regular || Size == SpotSize.Large || Size == SpotSize.Compact;
            case VehicleType.Bus:
                return Size == SpotSize.Large;
            default:
                return false;
        }
    }
}

/// <summary>
/// Ticket: issued on entry. Contains ticket id, vehicle, assigned spot, entry time, optionally exit time and fee.
/// </summary>
public class Ticket
{
    public string TicketId { get; set; }
    public string VehicleId { get; set; }
    public string VehicleLicense { get; set; }
    public VehicleType VehicleType { get; set; }
    public string SpotId { get; set; } //parking spot id
    public DateTime EntryTime { get; set; }
    public DateTime? ExitTime { get; set; }
    public decimal? PaidAmount { get; set; }

    public Ticket(Vehicle v, string spotId)
    {
        TicketId = Guid.NewGuid().ToString();
        VehicleId = v.Id;
        VehicleLicense = v.LicensePlate;
        VehicleType = v.Type;
        SpotId = spotId;
        EntryTime = DateTime.UtcNow;
        ExitTime = null;
        PaidAmount = null;
    }
}

/// <summary>
/// Receipt: returned on unpark; contains ticket id, parking duration, amount charged.
/// </summary>
public class Receipt
{
    public string TicketId { get; set; }
    public TimeSpan Duration { get; set; }
    public decimal Amount { get; set; }
    public DateTime PaidAt { get; set; }
}

/// <summary>
/// Interface for fee calculation strategies. Allows plugging multiple pricing policies.
/// will use it Demonstrates Strategy pattern.
/// </summary>
public interface IParkingFeeCalculator
{
    decimal CalculateFee(Ticket ticket, DateTime exitTime);
}

/// <summary>
/// SimpleHourlyFeeCalculator: charges per hour (rounded up) with different rates per vehicle type.
/// Demonstrates Strategy pattern.
/// </summary>
public class SimpleHourlyFeeCalculator : IParkingFeeCalculator
{
    private readonly Dictionary<VehicleType, decimal> _hourlyRates;

    public SimpleHourlyFeeCalculator()
    {
        /*_hourlyRates = new Dictionary<VehicleType, decimal>
        {
            { VehicleType.Motorcycle, 10m }, // currency units per hour
            { VehicleType.Car, 20m },
            { VehicleType.Bus, 50m }
        };*/
    }

    public decimal CalculateFee(Ticket ticket, DateTime exitTime)
    {
        var duration = exitTime - ticket.EntryTime;
        // Round up to nearest hour
        var hours = (int)Math.Ceiling(duration.TotalHours);
        if (hours <= 0) hours = 1;
        var rate = _hourlyRates[ticket.VehicleType];
        return rate * hours;
    }
}

/// <summary>
/// ParkingFloor: represents a floor containing multiple spots and helper methods to find and free spots.
/// </summary>
public class ParkingFloor
{
    public string FloorId { get; set; }
    public List<ParkingSpot> Spots { get; set; }    // public list as requested (no encapsulation)

    public ParkingFloor(string floorId, List<ParkingSpot> spots)
    {
        FloorId = floorId;
        Spots = spots ?? new List<ParkingSpot>();
    }

    /// <summary>
    /// Finds the first available spot that can fit the vehicle (naive nearest-first).
    /// Returns null if none found.
    /// </summary>
     // nearest-first simple allocation
    public ParkingSpot FindAvailableSpot(Vehicle vehicle)
    {
        return Spots.FirstOrDefault(s => !s.IsOccupied && s.CanFitVehicle(vehicle));
    }

    // mark spot occupied and attach ticketId
    public void OccupySpot(string spotId, string ticketId)
    {
        var spot = Spots.FirstOrDefault(s => s.Id == spotId); //Find the first parking spot in the list whose Id matches spotId. If no match is found, return null.
        if (spot == null) throw new ArgumentException("Invalid spotId");
        spot.IsOccupied = true;
        spot.CurrentTicketId = ticketId;
    }

    // free spot and clear ticket
    public void FreeSpot(string spotId)
    {
        var spot = Spots.FirstOrDefault(s => s.Id == spotId);
        if (spot == null) throw new ArgumentException("Invalid spotId");
        spot.IsOccupied = false;
        spot.CurrentTicketId = null;
    }

    // count available by size
    public int AvailableCountBySize(SpotSize size)
    {
        return Spots.Count(s => !s.IsOccupied && s.Size == size);
    }
}

/// <summary>
/// ParkingLot: Singleton representing the entire parking lot composed of floors.
/// Responsible for orchestrating spot allocation across floors.
/// </summary>
public class ParkingLot
{
    private static ParkingLot _instance;
    private readonly List<ParkingFloor> _floors;
    private readonly Dictionary<string, Ticket> _activeTickets; // ticketId -> Ticket
    private readonly IParkingFeeCalculator _feeCalculator;

    private ParkingLot(IParkingFeeCalculator feeCalculator)
    {
        _floors = new List<ParkingFloor>();
        _activeTickets = new Dictionary<string, Ticket>();
        _feeCalculator = feeCalculator ?? new SimpleHourlyFeeCalculator();
    }

    /// <summary>
    /// Singleton instance accessor. For interviews this is fine.
    /// </summary>
    public static ParkingLot GetInstance(IParkingFeeCalculator feeCalculator = null)
    {
        if (_instance == null)
        {
            _instance = new ParkingLot(feeCalculator);
        }
        return _instance;
    }

    // expose floors 
    public List<ParkingFloor> Floors => _floors;

    public void AddFloor(ParkingFloor floor)
    {
        _floors.Add(floor);
    }

    /// <summary>
    /// Park a vehicle: find spot across floors (floor order provided), create ticket and occupy the spot.
    /// Returns the Ticket if parked, otherwise null.
    /// </summary>
    public Ticket ParkVehicle(Vehicle vehicle)
    {
        foreach (var floor in _floors)
        {
            var spot = floor.FindAvailableSpot(vehicle);
            if (spot != null)
            {
                var ticket = new Ticket(vehicle, spot.Id);
                floor.OccupySpot(spot.Id, ticket.TicketId);
                _activeTickets[ticket.TicketId] = ticket;
                return ticket;
            }
        }
        // No spot found
        return null;
    }

    /// <summary>
    /// Unpark a vehicle given a ticketId. Calculates fee and frees the spot.
    /// Returns Receipt. Throws if ticket not found or already exited.
    /// </summary>
    public Receipt UnparkVehicle(string ticketId)
    {
        /*if (!_activeTickets.ContainsKey(ticketId))
            throw new ArgumentException("Invalid or unknown ticket.");*/

        var ticket = _activeTickets[ticketId];
       /* if (ticket.ExitTime.HasValue)
            throw new InvalidOperationException("Vehicle already exited.");*/

        var exitTime = DateTime.UtcNow;
        ticket.ExitTime = exitTime;

        // calculate fee
        var amount = _feeCalculator.CalculateFee(ticket, exitTime);
        ticket.PaidAmount = amount;

        // free spot which was occupied by the Vehcle
        // find floor containing the spot
        foreach (var floor in _floors)
        {
            var spot = floor.Spots.FirstOrDefault(s => s.Id == ticket.SpotId);
            if (spot != null)
            {
                floor.FreeSpot(spot.Id);
                break;
            }
        }

        // remove from active tickets
        _activeTickets.Remove(ticketId);

        return new Receipt
        {
            TicketId = ticketId,
            Duration = exitTime - ticket.EntryTime,
            Amount = amount,
            PaidAt = exitTime
        };
    }

    /// <summary>
    /// Shows output of overall availability grouped by floor and spot size to the user.
    /// </summary>
    public Dictionary<string, Dictionary<SpotSize, int>> GetAvailability()
    {
        var result = new Dictionary<string, Dictionary<SpotSize, int>>();
        foreach (var floor in _floors)
        {
            var bySize = new Dictionary<SpotSize, int>();
            foreach (SpotSize s in Enum.GetValues(typeof(SpotSize)))
            {
                bySize[s] = floor.AvailableCountBySize(s);
            }
            result[floor.FloorId] = bySize;
        }
        return result;
    }
}


/// <summary>
/// ParkingManager: higher-level facade for clients/tests. Uses ParkingLot singleton internally.
/// Exposes Park, Unpark, Query operations.
/// </summary>
public class ParkingManager
{
    private readonly ParkingLot _lot;

    public ParkingManager(ParkingLot lot)
    {
        _lot = lot;
    }

    /// <summary>
    /// Try to park and return ticket if successful; otherwise null.
    /// </summary>
    public Ticket Park(Vehicle vehicle)
    {
        return _lot.ParkVehicle(vehicle);
    }

    /// <summary>
    /// Unpark and return receipt.
    /// </summary>
    public Receipt Unpark(string ticketId)
    {
        return _lot.UnparkVehicle(ticketId);
    }

    public Ticket GetTicket(string ticketId) => _lot.GetActiveTicket(ticketId);

    public Dictionary<string, Dictionary<SpotSize, int>> GetAvailability() => _lot.GetAvailability();
}

/// <summary>
/// Demo Program: a small example showing how the system can be initialized and used.
/// </summary>
public class Program
{
    public static void Main()
    {
        // Create fee calculator and lot (singleton)
        var feeCalc = new SimpleHourlyFeeCalculator();
        var lot = ParkingLot.GetInstance(feeCalc);

        // Setup floors and spots (for demo, 2 floors)
        var floor1Spots = new List<ParkingSpot>();
        for (int i = 1; i <= 10; i++) // 10 compact
            floor1Spots.Add(new ParkingSpot($"F1-C{i}", SpotSize.Compact));
        for (int i = 1; i <= 20; i++) // 20 regular
            floor1Spots.Add(new ParkingSpot($"F1-R{i}", SpotSize.Regular));
        for (int i = 1; i <= 2; i++) // 2 large
            floor1Spots.Add(new ParkingSpot($"F1-L{i}", SpotSize.Large));
        var floor1 = new ParkingFloor("Floor-1", floor1Spots);

        var floor2Spots = new List<ParkingSpot>();
        for (int i = 1; i <= 8; i++)
            floor2Spots.Add(new ParkingSpot($"F2-C{i}", SpotSize.Compact));
        for (int i = 1; i <= 15; i++)
            floor2Spots.Add(new ParkingSpot($"F2-R{i}", SpotSize.Regular));
        for (int i = 1; i <= 4; i++)
            floor2Spots.Add(new ParkingSpot($"F2-L{i}", SpotSize.Large));
        var floor2 = new ParkingFloor("Floor-2", floor2Spots);

        lot.AddFloor(floor1);
        lot.AddFloor(floor2);

        var manager = new ParkingManager(lot);

        // Simulate parking
        var car = VehicleFactory.CreateVehicle(VehicleType.Car, "KA-01-AA-1234");
        var ticket = manager.Park(car);
        if (ticket != null)
        {
            Console.WriteLine($"Parked: TicketId={ticket.TicketId}, Spot={ticket.SpotId}, Entry={ticket.EntryTime}");
        }
        else
        {
            Console.WriteLine("No spot available for car.");
        }

        // Simulate waiting (in real tests, you'd mock time). For demo we won't wait.

        // Unpark after some time (for demonstration, we compute fee immediately)
        var receipt = manager.Unpark(ticket.TicketId);
        Console.WriteLine($"Unparked Ticket={receipt.TicketId}, Duration(Hours)={receipt.Duration.TotalHours:F2}, Amount={receipt.Amount}");

        // Query availability
        var avail = manager.GetAvailability();
        foreach (var floorEntry in avail)
        {
            Console.WriteLine($"Availability for {floorEntry.Key}:");
            foreach (var kv in floorEntry.Value)
            {
                Console.WriteLine($"  {kv.Key}: {kv.Value}");
            }
        }
    }
}


