package Server.Interface;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

/** 
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean 
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface IResourceManager
{
    /**
     * Add seats to a flight.
     *
     * In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return Success
     */
    public Serializable addFlight(Command cmd, Vector<String> arguments);
    
    /**
     * Add car at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public Serializable addCars(Command cmd, Vector<String> arguments);
   
    /**
     * Add room at a location.
     *
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     *
     * @return Success
     */
    public Serializable addRooms(Command cmd, Vector<String> arguments);
			    
    /**
     * Add customer.
     *
     * @return Unique customer identifier
     */
    public Serializable newCustomer(Command cmd, Vector<String> arguments);
    
    /**
     * Add customer with id.
     *
     * @return Success
     */
    public Serializable newCustomerId(Command cmd, Vector<String> arguments);

    /**
     * Delete the flight.
     *
     * deleteFlight implies whole deletion of the flight. If there is a
     * reservation on the flight, then the flight cannot be deleted
     *
     * @return Success
     */   
    public Serializable deleteFlight(Command cmd, Vector<String> arguments);
    
    /**
     * Delete all cars at a location.
     *
     * It may not succeed if there are reservations for this location
     *
     * @return Success
     */		    
    public Serializable deleteCars(Command cmd, Vector<String> arguments);

    /**
     * Delete all rooms at a location.
     *
     * It may not succeed if there are reservations for this location.
     *
     * @return Success
     */
    public Serializable deleteRooms(Command cmd, Vector<String> arguments);
    
    /**
     * Delete a customer and associated reservations.
     *
     * @return Success
     */
    public Serializable deleteCustomer(Command cmd, Vector<String> arguments);

    /**
     * Query the status of a flight.
     *
     * @return Number of empty seats
     */
    public Serializable queryFlight(Command cmd, Vector<String> arguments);

    /**
     * Query the status of a car location.
     *
     * @return Number of available cars at this location
     */
    public Serializable queryCars(Command cmd, Vector<String> arguments);

    /**
     * Query the status of a room location.
     *
     * @return Number of available rooms at this location
     */
    public Serializable queryRooms(Command cmd, Vector<String> arguments);

    /**
     * Query the customer reservations.
     *
     * @return A formatted bill for the customer
     */
    public Serializable queryCustomerInfo(Command cmd, Vector<String> arguments);
    
    /**
     * Query the status of a flight.
     *
     * @return Price of a seat in this flight
     */
    public Serializable queryFlightPrice(Command cmd, Vector<String> arguments);

    /**
     * Query the status of a car location.
     *
     * @return Price of car
     */
    public Serializable queryCarsPrice(Command cmd, Vector<String> arguments);

    /**
     * Query the status of a room location.
     *
     * @return Price of a room
     */
    public Serializable queryRoomsPrice(Command cmd, Vector<String> arguments);

    /**
     * Reserve a seat on this flight.
     *
     * @return Success
     */
    public Serializable reserveFlight(Command cmd, Vector<String> arguments);

    /**
     * Reserve a car at this location.
     *
     * @return Success
     */
    public Serializable reserveCar(Command cmd, Vector<String> arguments);

    /**
     * Reserve a room at this location.
     *
     * @return Success
     */
    public Serializable reserveRoom(Command cmd, Vector<String> arguments);

    /**
     * Reserve a bundle for the trip.
     *
     * @return Success
     */
    public Serializable bundle(Command cmd, Vector<String> arguments);

    /**
     * Convenience for probing the resource manager.
     *
     * @return Name
     */
    public String getName();
}
