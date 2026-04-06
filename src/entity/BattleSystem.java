package entity;

import java.util.Random;

public class BattleSystem {

    public enum Move {
        ROCK, PAPER, SCISSORS
    }

    public enum BattleResult {
        PLAYER_WIN, ENEMY_WIN, DRAW
    }

    private static final Random random = new Random();

    public static Move getRandomEnemyMove() {
        Move[] moves = Move.values();
        return moves[random.nextInt(moves.length)];
    }

    public static BattleResult resolve(Move playerMove, Move enemyMove) {
        if (playerMove == enemyMove) return BattleResult.DRAW;

        boolean playerWins =
                (playerMove == Move.ROCK     && enemyMove == Move.SCISSORS) ||
                        (playerMove == Move.SCISSORS && enemyMove == Move.PAPER)    ||
                        (playerMove == Move.PAPER    && enemyMove == Move.ROCK);

        return playerWins ? BattleResult.PLAYER_WIN : BattleResult.ENEMY_WIN;
    }

    public static String getMoveEmoji(Move move) {
        return switch (move) {
            case ROCK -> "ROCK";
            case PAPER -> "PAPER";
            case SCISSORS -> "SCISSORS";
        };
    }

    public static String getResultText(BattleResult result) {
        return switch (result) {
            case PLAYER_WIN -> "You Win!";
            case ENEMY_WIN  -> "You Lose!";
            case DRAW       -> "Draw!";
        };
    }
}