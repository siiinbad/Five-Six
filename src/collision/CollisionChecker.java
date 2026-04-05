package collision;

import entity.Entity;
import main.GamePanel;
import java.awt.Rectangle;
import java.util.List;

public class CollisionChecker {

    GamePanel gp;

    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    // Check collision with screen edges
    public void checkWall(Entity entity) {

        int nextX = entity.worldX;
        int nextY = entity.worldY;

        switch(entity.direction) {
            case "up" -> nextY -= entity.speed;
            case "down" -> nextY += entity.speed;
            case "left" -> nextX -= entity.speed;
            case "right" -> nextX += entity.speed;
        }

        int left = nextX + entity.solidArea.x;
        int right = left + entity.solidArea.width;
        int top = nextY + entity.solidArea.y;
        int bottom = top + entity.solidArea.height;

        if (left < 0 || top < 0 || right > gp.screenWidth || bottom > gp.screenHeight) {
            entity.collisionOn = true;
        }
    }

    // Check collision with a list of rectangles (walls, objects)
    public void checkObstacles(Entity entity, List<Rectangle> obstacles) {

        int nextX = entity.worldX;
        int nextY = entity.worldY;

        switch(entity.direction) {
            case "up" -> nextY -= entity.speed;
            case "down" -> nextY += entity.speed;
            case "left" -> nextX -= entity.speed;
            case "right" -> nextX += entity.speed;
        }

        // Create the predicted hitbox
        Rectangle futureBox = new Rectangle(
            nextX + entity.solidArea.x,
            nextY + entity.solidArea.y,
            entity.solidArea.width,
            entity.solidArea.height
        );

        // Reset collision
        entity.collisionOn = false;

        for (Rectangle obstacle : obstacles) {
            if (futureBox.intersects(obstacle)) {
                entity.collisionOn = true;
                break;
            }
        }
    }

    // Single obstacle version (optional convenience)
    public boolean willCollide(Entity entity, Rectangle obstacle) {
        int nextX = entity.worldX;
        int nextY = entity.worldY;

        switch(entity.direction) {
            case "up" -> nextY -= entity.speed;
            case "down" -> nextY += entity.speed;
            case "left" -> nextX -= entity.speed;
            case "right" -> nextX += entity.speed;
        }

        Rectangle futureBox = new Rectangle(
            nextX + entity.solidArea.x,
            nextY + entity.solidArea.y,
            entity.solidArea.width,
            entity.solidArea.height
        );

        return futureBox.intersects(obstacle);
    }
}