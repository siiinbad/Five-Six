package entity;

import static main.HitboxColors.Map.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.HitboxColors;
import main.KeyHandler;

public class Player extends Entity {
    public GamePanel gp;
    KeyHandler keyH;
    public String characterName;
    boolean nearInteractable = false;
    boolean eWasPressed  = false;

    public CharacterStats.CharacterType charType;
    public double damageMultiplier;
    public int spawnX, spawnY;

    public Player(GamePanel gp, KeyHandler keyH, String characterName) {
        this.gp = gp;
        this.keyH = keyH;
        this.characterName = characterName;
        charType = CharacterStats.CharacterType.fromName(characterName);
        damageMultiplier = charType.damageMultiplier;
        setHP(charType.maxHP);
        speed = 4;
        direction = "down";
        loadImages();
    }

    public void saveSpawn(int x, int y) { spawnX = x; spawnY = y; }

    public void respawnWithPenalty() {
        x = spawnX; y = spawnY;
        setHPPercent(0.30);
    }

    private void loadImages() {
        try {
            String p = "/res/sprites/player/" + characterName + "/" + characterName + "_";
            up1    = read(p+"back_walk1.png");   up2    = read(p+"back_walk2.png");
            down1  = read(p+"front_walk1.png");  down2  = read(p+"front_walk2.png");
            left1  = read(p+"left_walk1.png");   left2  = read(p+"left_walk2.png");
            right1 = read(p+"right_walk1.png");  right2 = read(p+"right_walk2.png");
            standUp    = read(p+"back_stand.png");
            standDown  = read(p+"front_stand.png");
            standLeft  = read(p+"left_stand.png");
            standRight = read(p+"right_stand.png");
        } catch (Exception e) { System.out.println("Player images failed."); }
    }

    private BufferedImage read(String path) throws Exception {
        return ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    public void update() {
        if (gp.gameState == GamePanel.battleState || gp.gameState == GamePanel.fadeState) return;
        int nx=x, ny=y; boolean moving=false;
        if      (keyH.upPressed)    { direction="up";    ny-=speed; moving=true; }
        else if (keyH.downPressed)  { direction="down";  ny+=speed; moving=true; }
        else if (keyH.leftPressed)  { direction="left";  nx-=speed; moving=true; }
        else if (keyH.rightPressed) { direction="right"; nx+=speed; moving=true; }

        if (moving && !isColliding(nx,ny)) {
            x=nx; y=ny;
            if (++spriteCounter > 12) { spriteNum=(spriteNum==1)?2:1; spriteCounter=0; }
        }
        checkInteractions();
    }

    private boolean isColliding(int nx, int ny) {
        if (gp.hitboxImage == null) return false;
        Rectangle playerBox = getPlayerCollisionBox(nx, ny);
        if (areaContainsColor(playerBox, COLOR_WALL)) return true;
        for (int enemyColor : getEnemyColors()) {
            if (!gp.enemyStats.isDefeated(enemyColor) && areaContainsColor(playerBox, enemyColor)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNPCColor(int c) {
        return HitboxColors.isNpcColor(c);
    }

    private void checkInteractions() {
        nearInteractable = false;
        if (gp.hitboxImage == null) return;
        Rectangle playerBox = getPlayerCollisionBox(x, y);
        Rectangle interactBox = getInteractionBox(playerBox);
        int color = findInteractionColor(interactBox);
        if (color == 0) color = findInteractionColor(playerBox);

        boolean interactable = isNPCColor(color) || color==COLOR_NEXTAREA;
        if (isNPCColor(color) && gp.enemyStats.isDefeated(color)) {
            gp.currentDialog=""; return;
        }
        if (!interactable) {
            if (!gp.currentDialog.isEmpty()) { gp.currentDialog=""; gp.dialogStage=0; gp.lastNPCColor=0; }
            return;
        }
        nearInteractable = true;
        if (gp.lastNPCColor != color) { gp.dialogStage=0; gp.lastNPCColor=color; }

        if (keyH.ePressed && !eWasPressed) {
            eWasPressed = true;
            handleInteract(color);
        }
        if (!keyH.ePressed) eWasPressed = false;
    }

    private Rectangle getPlayerCollisionBox(int px, int py) {
        return new Rectangle(px + 45, py + 40, 70, gp.tileSize - 60);
    }

    private Rectangle getInteractionBox(Rectangle playerBox) {
        Rectangle interactionBox = new Rectangle(playerBox);
        int interactDistance = gp.tileSize / 3;
        switch (direction) {
            case "up" -> interactionBox.translate(0, -interactDistance);
            case "down" -> interactionBox.translate(0, interactDistance);
            case "left" -> interactionBox.translate(-interactDistance, 0);
            case "right" -> interactionBox.translate(interactDistance, 0);
        }
        interactionBox.grow(10, 10);
        return interactionBox;
    }

    private boolean areaContainsColor(Rectangle screenRect, int targetColor) {
        Rectangle imageRect = toImageRect(screenRect);
        if (imageRect == null) return false;
        for (int iy = imageRect.y; iy < imageRect.y + imageRect.height; iy++) {
            for (int ix = imageRect.x; ix < imageRect.x + imageRect.width; ix++) {
                if ((gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF) == targetColor) return true;
            }
        }
        return false;
    }

    private int findInteractionColor(Rectangle screenRect) {
        Rectangle imageRect = toImageRect(screenRect);
        if (imageRect == null) return 0;
        for (int iy = imageRect.y; iy < imageRect.y + imageRect.height; iy++) {
            for (int ix = imageRect.x; ix < imageRect.x + imageRect.width; ix++) {
                int color = gp.hitboxImage.getRGB(ix, iy) & 0xFFFFFF;
                if (color == COLOR_NEXTAREA || isNPCColor(color)) return color;
            }
        }
        return 0;
    }

    private Rectangle toImageRect(Rectangle screenRect) {
        if (gp.hitboxImage == null || gp.getWidth() <= 0 || gp.getHeight() <= 0) return null;
        int imageWidth = gp.hitboxImage.getWidth();
        int imageHeight = gp.hitboxImage.getHeight();
        int left = Math.max(0, screenRect.x * imageWidth / gp.getWidth());
        int top = Math.max(0, screenRect.y * imageHeight / gp.getHeight());
        int right = Math.min(imageWidth - 1, ((screenRect.x + screenRect.width - 1) * imageWidth) / gp.getWidth());
        int bottom = Math.min(imageHeight - 1, ((screenRect.y + screenRect.height - 1) * imageHeight) / gp.getHeight());
        if (left > right || top > bottom) return null;
        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private void handleInteract(int color) {
        // Broken door — just show message, no battle
        if (color == COLOR_BROKENDOOR) {
            gp.currentDialog = "The door seems to be broken... it won't open.";
            return;
        }
        // Next area transitions
        if (color == COLOR_NEXTAREA) {
            if (gp.isGleMap()) {
                if (gp.enemyStats.isDefeated(COLOR_JOHNRU)) {
                    gp.loadMapImages(GamePanel.FRONTGATE_MAP);
                } else { gp.currentDialog="Defeat Johnru first to proceed."; }
            }
            return;
        }
        // Miniboss gate checks
        if (color==COLOR_JOHNRU && !gp.allOtherGleEnemiesDefeated()) {
            gp.currentDialog="Johnru: Come back when you've beaten the others."; return;
        }
        if (color==COLOR_VAUGHN && !gp.allOtherFrontgateEnemiesDefeated()) {
            gp.currentDialog="Vaughn: Take care of my boys first."; return;
        }
        // Advance dialog
        gp.dialogStage++;
        setDialog(color);
        if (isFinalStage(color)) {
            gp.pendingBattleEnemyColor=color;
            gp.startFadeToBlack();
        }
    }

    private boolean isFinalStage(int c) {
        if (c==COLOR_JAMES||c==COLOR_KYLE)        return gp.dialogStage>=2;
        if (c==COLOR_ALIEYANDREW||c==COLOR_ADRIAN) return gp.dialogStage>=3;
        if (c==COLOR_JOHNRU) return gp.allOtherGleEnemiesDefeated() && gp.dialogStage>=2;
        if (c==COLOR_DARRYLL||c==COLOR_GIO||c==COLOR_YOHANN) return gp.dialogStage>=2;
        if (c==COLOR_DIRK||c==COLOR_JAKE)         return gp.dialogStage>=3;
        if (c==COLOR_VAUGHN) return gp.allOtherFrontgateEnemiesDefeated() && gp.dialogStage>=2;
        return false;
    }

    private int[] getEnemyColors() {
        return HitboxColors.enemyColors();
    }

    private void setDialog(int c) {
        String name = characterName;
        if      (c==COLOR_JAMES) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "James: Yo "+name+", you want your money back?";
                case 2 -> "James: Okay, but you got to win first. Let's battle!";
                default -> "";
            };
        } else if (c==COLOR_ALIEYANDREW) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Alieyandrew: Oh hi there, "+name+"!";
                case 2 -> "Alieyandrew: So you want your money back?";
                case 3 -> "Alieyandrew: Okay, beat me and I'll give it to you.";
                default -> "";
            };
        } else if (c==COLOR_KYLE) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Kyle: Oi!! "+name+", you're here to get your money?";
                case 2 -> "Kyle: HAHAHA! Come beat me first!";
                default -> "";
            };
        } else if (c==COLOR_JOHNRU) {
            if (!gp.allOtherGleEnemiesDefeated()) {
                gp.currentDialog="Johnru: Come back when you've beaten the others."; gp.dialogStage=0; return;
            }
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Johnru: Oh, you think you're strong now, "+name+"?";
                case 2 -> "Johnru: Let's go then — I'll let you get out of the room.";
                default -> "";
            };
        } else if (c==COLOR_ADRIAN) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Adrian: Hi there "+name+", class is over.";
                case 2 -> "Adrian: Oh, you want your money I owe you?";
                case 3 -> "Adrian: Okay, no problem — let's play a game first.";
                default -> "";
            };
        } else if (c==COLOR_DARRYLL) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Darryll: Oh? You came all the way here for your money, "+name+"?";
                case 2 -> "Darryll: You'll have to pry it from my hands!";
                default -> "";
            };
        } else if (c==COLOR_GIO) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Gio: Don't even bother, "+name+". You won't win.";
                case 2 -> "Gio: Alright, let's settle this the hard way.";
                default -> "";
            };
        } else if (c==COLOR_YOHANN) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Yohann: Hey "+name+", long time no see!";
                case 2 -> "Yohann: Sorry but I'm keeping your money unless you beat me.";
                default -> "";
            };
        } else if (c==COLOR_DIRK) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Dirk: Hmph. So you made it this far, "+name+".";
                case 2 -> "Dirk: Don't think I'll go easy on you.";
                case 3 -> "Dirk: En garde.";
                default -> "";
            };
        } else if (c==COLOR_JAKE) {
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Jake: Wooah, "+name+"! You're really here?";
                case 2 -> "Jake: I respect the hustle. Let's battle!";
                case 3 -> "Jake: May the best one win!";
                default -> "";
            };
        } else if (c==COLOR_VAUGHN) {
            if (!gp.allOtherFrontgateEnemiesDefeated()) {
                gp.currentDialog="Vaughn: Take care of my crew first."; gp.dialogStage=0; return;
            }
            gp.currentDialog = switch(gp.dialogStage) {
                case 1 -> "Vaughn: So you really made it, "+name+". Impressive.";
                case 2 -> "Vaughn: But this ends here. Let's go!";
                default -> "";
            };
        }
    }

    public void draw(Graphics2D g2) {
        boolean moving = keyH.upPressed||keyH.downPressed||keyH.leftPressed||keyH.rightPressed;
        BufferedImage im = switch (direction) {
            case "up"    -> moving ? (spriteNum==1?up1:up2)     : standUp;
            case "down"  -> moving ? (spriteNum==1?down1:down2) : standDown;
            case "left"  -> moving ? (spriteNum==1?left1:left2) : standLeft;
            default      -> moving ? (spriteNum==1?right1:right2): standRight;
        };
        // Shadow
        g2.setColor(new Color(0,0,0,100));
        int sw=(int)(gp.tileSize*.6), sh=(int)(gp.tileSize*.15);
        g2.fillOval(x+(gp.tileSize-sw)/2, y+gp.tileSize-sh-15, sw, sh);
        g2.drawImage(im, x, y, gp.tileSize, gp.tileSize, null);

        if (gp.showDebug) {
            g2.setColor(Color.RED);
            g2.drawRect(x+gp.tileSize/2-35, y+40, 70, gp.tileSize-60);
        }
        if (nearInteractable && gp.currentDialog.isEmpty()) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial",Font.BOLD,20));
            g2.drawString("Press E to talk", x+10, y-10);
        }
    }
}
