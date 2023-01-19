package lab4.gamehandler.game;

import lab4.SnakesProto;
import lab4.datatransfer.NetNode;
import lab4.utils.PlayerUtils;
import lombok.Getter;
import org.apache.log4j.Logger;
import lab4.gamehandler.GameState;
import lab4.gamehandler.Player;
import lab4.gamehandler.Coord;
import lab4.gamehandler.Snake;
import lab4.master.MasterHandler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Game implements GameHandler {
    private static final Logger logger = Logger.getLogger(Game.class);

    private static final int SIZE_OF_EMPTY_SQUARE_FOR_SNAKE = 5;
    private static final String UNKNOWN_PLAYER_ERROR_MESSAGE = "Unknown player";
    private static final double PROBABILITY = 0.5;

    @Getter private final Map<Player, Snake> playersWithSnakes = new HashMap<>();
    private final Map<Player, Snake> playersForRemove = new HashMap<>();
    private final List<Snake> zombieSnakes = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private static final Random random = new Random();

    @Getter private final SnakesProto.GameConfig config;
    private final GameField field;
    private final List<Cell> foods;
    private final MasterHandler masterHandler;

    private int stateID;
    private int playerIDCounter = 1;

    public Game(SnakesProto.GameConfig config, MasterHandler masterHandler) {
        this.config = config;
        this.masterHandler = masterHandler;
        this.field = new GameField(this.config.getWidth(), this.config.getHeight());
        this.stateID = 0;
        this.foods = new ArrayList<>(this.config.getFoodStatic());
        generateFoods();
    }

    public Game(GameState state, MasterHandler masterHandler) {
        this.masterHandler = masterHandler;
        config = state.getGameConfig();
        field = new GameField(config.getWidth(), config.getHeight());
        stateID = state.getStateID() + 1;

        state.getSnakes().forEach(snake -> {
            snake.getCoordinates().forEach(point -> field.set(point, CellType.SNAKE));
            switch (snake.getState()) {
                case ZOMBIE -> zombieSnakes.add(snake);
                case ALIVE -> {
                    Player snakeOwner = Optional.ofNullable(PlayerUtils.findPlayerBySnake(snake, state.getActivePlayers()))
                            .orElseThrow(() -> new IllegalStateException("Can't get player from alive snake"));
                    playersWithSnakes.put(snakeOwner, snake);
                }
            }
        });

        for (Player player : state.getActivePlayers()) {
            players.add(player);
            if (player.getId() >= playerIDCounter) {
                playerIDCounter = player.getId() + 1;
            }
        }

        foods = new ArrayList<>(state.getFoods().size());
        for (Coord fruit : state.getFoods()) {
            field.set(fruit, CellType.FOOD);
            foods.add(new Cell(fruit, CellType.FOOD));
        }
    }

    @Override
    public Player registerNewPlayer(String playerName, NetNode netNode) {
        Player player = new Player(playerName, playerIDCounter, netNode);
        playerIDCounter++;

        List<Cell> snakeCoords = createNewSnake();
        if (snakeCoords.isEmpty()) {
            throw new IllegalStateException("Cant add new player because no space on field");
        }
        Coord head = snakeCoords.get(0).getCoord();
        Coord tail = snakeCoords.get(1).getCoord();
        Snake playerSnake = new Snake(head, tail, field.getWidth(), field.getHeight());

        playerSnake.setPlayerID(player.getId());
        snakeCoords.forEach(cell -> field.set(cell.getY(), cell.getX(), CellType.SNAKE));

        players.add(player);
        playersWithSnakes.put(player, playerSnake);
        return player;
    }

    @Override
    public void removePlayer(Player player) {
        if (playersWithSnakes.containsKey(player)) {
            Snake snake = playersWithSnakes.get(player);
            snake.setState(SnakesProto.GameState.Snake.SnakeState.ZOMBIE);
            zombieSnakes.add(snake);
            markPlayerInactive(player);
        }
    }

    @Override
    public void moveAllSnakes(Map<Player, SnakesProto.Direction> playersMoves) {
        playersWithSnakes.keySet().forEach(player -> makeMove(player, playersMoves.getOrDefault(player, null)));
        zombieSnakesMove();
        generateFoods();

        playersForRemove.keySet().forEach(player -> {
            makeFoodsFromSnakeWithProbability(playersWithSnakes.get(player));
            markPlayerInactive(player);
        });
        playersForRemove.clear();
        masterHandler.update(generateGameState());
    }

    @Override
    public Snake getSnakeByPlayer(Player player) {
        return playersWithSnakes.get(player);
    }

    private List<Cell> createNewSnake() {
        Optional<Cell> centerOfEmptySquareOnField = field.findCenterOfSquareWithOutSnake(SIZE_OF_EMPTY_SQUARE_FOR_SNAKE);
        if (centerOfEmptySquareOnField.isEmpty()) {
            return Collections.emptyList();
        }

        Cell head = centerOfEmptySquareOnField.get();
        Optional<Cell> tail =
                Stream.of(
                        field.get(head.getY() - 1, head.getX()),
                        field.get(head.getY() + 1, head.getX()),
                        field.get(head.getY(), head.getX() - 1),
                        field.get(head.getY(), head.getX() + 1)
                )
                .filter(cell -> CellType.EMPTY.equals(cell.getType()))
                .findFirst();
        if (tail.isEmpty()) {
            return Collections.emptyList();
        }

        return List.of(head, tail.get());
    }

    private void markPlayerInactive(Player player) {
        playersWithSnakes.remove(player);
        players.remove(player);
    }

    private void makeMove(Player player, SnakesProto.Direction direction) {
        if (!playersWithSnakes.containsKey(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }

        Snake snake = playersWithSnakes.get(player);
        snake.makeMove(direction);

        if (isSnakeCrashed(snake)) {
            handlePlayerLose(player, snake);
            return;
        }
        if (isSnakeAteFood(snake)) {
            incrementScore(player);
            removeFood(snake.getHead());
        }
        else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void removeFood(Coord fruitForRemove) {
        foods.removeIf(fruit -> fruitForRemove.equals(fruit.getCoord()));
    }

    private void handlePlayerLose(Player player, Snake playerSnake) {
        playersForRemove.put(player, playerSnake);
    }

    private void zombieSnakesMove() {
        zombieSnakes.forEach(this::zombieMove);
        zombieSnakes.stream().filter(this::isSnakeCrashed).forEach(this::makeFoodsFromSnakeWithProbability);
        zombieSnakes.removeIf(this::isSnakeCrashed);
    }

    private void zombieMove(Snake snake) {
        snake.makeMove();
        if (isSnakeAteFood(snake)) {
            removeFood(snake.getHead());
        }
        else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void generateFoods() {
        int aliveSnakesCount = playersWithSnakes.size();
        int requiredFruitsNumber = config.getFoodStatic() + aliveSnakesCount;
        if (foods.size() == requiredFruitsNumber) return;
        if (field.getEmptyCellsCount() < requiredFruitsNumber) {
            logger.debug("Can't generate required number of fruits=" + requiredFruitsNumber + ", empty cells number=" + field.getEmptyCellsCount());
            requiredFruitsNumber = field.getEmptyCellsCount();
        }

        while (foods.size() < requiredFruitsNumber) {
            Cell randomEmptyCell = field.findRandomEmptyCell().orElseThrow(() -> new IllegalStateException("Can't find empty cell"));
            field.set(randomEmptyCell.getCoord(), CellType.FOOD);
            foods.add(randomEmptyCell);
        }
    }

    private void incrementScore(Player player) {
        if (!players.contains(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }
        player.incrementScore();
    }

    private boolean isSnakeAteFood(Snake snake) {
        Coord snakeHead = snake.getHead();
        return foods.stream().anyMatch(fruit -> snakeHead.equals(fruit.getCoord()));
    }

    private void makeFoodsFromSnakeWithProbability(Snake snake) {
        for (Coord p : snake.getCoordinates()) {
            if (p.equals(snake.getHead())) {
                continue;
            }
            if (random.nextDouble() < PROBABILITY) {
                field.set(p, CellType.FOOD);
                foods.add(field.get(p.y(), p.x()));
            }
            else {
                field.set(p, CellType.EMPTY);
            }
        }
    }

    private boolean isSnakeCrashed(Snake snake) {
        if (isSnakeCrashedToZombie(snake)) {
            return true;
        }
        for (Map.Entry<Player, Snake> playerWithSnake : playersWithSnakes.entrySet()) {
            Snake otherSnake = playerWithSnake.getValue();
            if (checkCrashIntoYourself(snake)) {
                return true;
            }
            if (otherSnake != snake && otherSnake.isSnake(snake.getHead())) {
                incrementScore(playerWithSnake.getKey());
                return true;
            }
        }
        return false;
    }

    private boolean isSnakeCrashedToZombie(Snake snake) {
        return zombieSnakes.stream().anyMatch(zombieSnake -> zombieSnake != snake && zombieSnake.isSnake(snake.getHead()));
    }

    private boolean checkCrashIntoYourself(Snake snake) {
        return snake.isSnakeBody(snake.getHead()) || snake.getTail().equals(snake.getHead());
    }

    private GameState generateGameState() {
        int currentStateID = stateID++;
        return new GameState(getFoodsAsPointsList(), new ArrayList<>(players), generateSnakeList(), config, currentStateID);
    }

    private List<Coord> getFoodsAsPointsList() {
        return foods.stream().map(Cell::getCoord).collect(Collectors.toList());
    }

    private List<Snake> generateSnakeList() {
        List<Snake> snakes = new ArrayList<>(playersWithSnakes.size() + zombieSnakes.size());
        playersWithSnakes.forEach((player, snake) -> snakes.add(snake));
        snakes.addAll(zombieSnakes);
        return snakes;
    }
}
