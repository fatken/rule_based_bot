// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;
//import javafx.scene.web.HTMLEditorSkin.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import java.util.Arrays;


// public class RandomCollection<E> {
//     private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
//     private final Random random;
//     private double total = 0;

//     public RandomCollection() {
//         this(new Random());
//     }

//     public RandomCollection(Random random) {
//         this.random = random;
//     }

//     public RandomCollection<E> add(double weight, E result) {
//         if (weight <= 0) return this;
//         total += weight;
//         map.put(total, result);
//         return this;
//     }

//     public E next() {
//         double value = random.nextDouble() * total;
//         return map.higherEntry(value).getValue();
//     }
// }




public class MyBot {

    public static int MAX_NO_SHIPS = 1;
    
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        HashMap<EntityId, String> ship_status = new HashMap<>();


        Game game = new Game();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        for (;;) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;
            int noShips = me.ships.size();
            // Log.log("Type of me.ships: " + me.ships.getClass());
            // Log.log("length of me.ships: " + me.ships.size());



            // Game turnNumber = game.turnNumber;
            // Log.log("Turn number: " + turnNumber);

            final ArrayList<Command> commandQueue = new ArrayList<>();



            // iterate thru ships
            // ship can hold up to 1000 halite
            // Each turn the ship collects 25% of halite on its location.
            for (final Ship ship : me.ships.values()) { 
                int max = Constants.MAX_HALITE; // 1000, max of halite a ship can hold
                // Log.log(Integer.toString(max)); // Integer.toString() or String.valueOf() needed to do primitive int type conversion
                // log halite amount in each ship
                Log.log("Ship" + ship + " has " + ship.halite + " halite ");
                // ship.id is of type EntityId. Needs to convert to string with .toString().
                // Log.log("Ship id: " + ship.id.toString());

                // Position position = GameMap.at(ship);
                // Log.log("Ship position halite amount: " + GameMap.at(position).halite);
                // Log.log("Ship pos halite: " + gameMap.at(ship).halite);

                // ship status initialization
                if(!ship_status.containsKey(ship.id)){
                    ship_status.put(ship.id, "exploring");
                }
                
                // if ship is full and has status "returning"
                if(ship_status.get(ship.id) == "returning"){
                    Log.log("ship is returning");
                    // if ship is back at shipyard
                    Log.log("ship pos: " + ship.position);
                    Log.log("shipyard pos: " + me.shipyard.position);

                    if(ship.position.equals(me.shipyard.position)){
                        // return to exploring
                        Log.log("ship is at shipyard, return to exploring");
                        ship_status.put(ship.id, "exploring");
                        Log.log("condition changed to : " + ship_status.get(ship.id));
                    }
                    // else if not yet at shipyard
                    else{
                        // let it move back to shipyard
                        Log.log("navigating back to shipyard");
                        final Direction move = gameMap.naiveNavigate(ship, me.shipyard.position);
                        commandQueue.add(ship.move(move));
                        ;
                    }
                }
                // else if ship exploring
                else{
                    // if ship contains enough halite, change condition back to "returning"
                    Log.log("Ship exploring");
                    Log.log("Status : " + ship_status.get(ship.id));
                    if(ship.halite >= (Constants.MAX_HALITE)){
                        Log.log("Ship collected full halite, changing condition to return");
                        ship_status.put(ship.id, "returning");
                        Log.log("Status : " + ship_status.get(ship.id));
                    }
                    // if ship exploring
                    else{
                        // if ship position contains less than 1/10 of maxi halite
                        if (gameMap.at(ship).halite < Constants.MAX_HALITE / 10) {
                            // random move
                            Log.log("No enough halite for exploring, random moving");

                            // generate surrounding positions
                            ArrayList<Position> surroundingPositions = ship.position.getSurroundingCardinals();
                            // find most fertile surrounding positions
                            // Moving across heavy concentrations of halite costs more. The movement cost is 10% of the halite in the position
                            // the ship is moving into. 
                            // TRAVELING ON LOWER HALITE CELLS IS MORE EFFICIENT
                            // IT IS MOTIVATED TO HARVEST IN A FOUR-CELL RADIUS with 2 or more nearby opposing player. It becomes inspired. When
                            // inspired, the ship collects halite and receives an additional 200% bonus.

                            int surroundingHaliteMaxPosIdx = 0; // the index has range 0-3, representing NORTH, SOUTH, EAST and WEST
                            int surroundingHaliteMax = 0;
                            for (int i = 0; i < surroundingPositions.size(); i++){
                                int tmpHalite = gameMap.at(surroundingPositions.get(i)).halite;
                                if ( tmpHalite > surroundingHaliteMax){
                                    surroundingHaliteMaxPosIdx = i;
                                    surroundingHaliteMax = tmpHalite;
                                }
                            }
                            Log.log("surrounding max halite: " + surroundingHaliteMax + ", pos idx: " + surroundingHaliteMaxPosIdx);




                            Log.log("Moving to most fertile pos.");
                            final Direction randomDirection = Direction.ALL_CARDINALS.get(surroundingHaliteMaxPosIdx); //rng.nextInt(4)
                            commandQueue.add(ship.move(randomDirection));
                        }
                        // else, harvesting
                        else {
                            commandQueue.add(ship.stayStill());
                            Log.log("Lots of halite to harvest, Staying still.");
                        }
                    }
                }

                
                // ****************
                // 1.
                // directionalOffset() returns the position NORTH, SOUTH, EAST, WEST or STILL case of current position
                // Log.log("ship pos: " + ship.position.directionalOffset(case)); // NORTH, SOUTH, EAST, WEST, STILL
                // 2.
                // ship.position.getSurroundingCardinals();
                // Used along with directionalOffset() to generate positions adjacent to the position. 
                // For all cardinals NORTH, SOUTH, EAST, WEST and STILL, iteratively call directionalOffset() to generate
                // a list of cardinals and return the list of cardinals.



                // ******************
                // SHIP DROPOFF related codes
                // commandQueue.add(ship.makeDropoff());
                // get a list of dropoffs
                // me.get_dropoffs();
                // calculate manhattan distance
                // game_map.calculate_distance(ship.position, dropoff.position);
                // ******************



            }

            // Log.log("Ship sum: " + size(me.get_ships().size()));
            if (
                game.turnNumber <= 200 &&
                me.halite >= Constants.SHIP_COST &&
                !gameMap.at(me.shipyard).isOccupied()&& noShips < MAX_NO_SHIPS)
            {
                commandQueue.add(me.shipyard.spawn());
            }

            game.endTurn(commandQueue);
        }
    }
}
