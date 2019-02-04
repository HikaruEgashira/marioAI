package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;

import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.engine.sprites.Sprite;
import ch.idsia.benchmark.mario.environments.Environment;

public class task2 extends BasicMarioAIAgent implements Agent {
  public task2() {
    super("task2");
  }

  private boolean isObstacle(int c) {
    switch (c) {
      case GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH:
      case GeneralizerLevelScene.BRICK:
      case GeneralizerLevelScene.FLOWER_POT_OR_CANNON:
        return true;
      default:
        return false;
    }
  }

  private boolean isHole(int c) {
    boolean isHoleFlag = true;
    for (int i = 0; i < 19; i++) {
      isHoleFlag = isHoleFlag ? (levelScene[i][c] == 0) : false;
    }
    return isHoleFlag;
  }

  private boolean isCreature(int c) {
    switch (c) {
      case Sprite.KIND_GOOMBA:
      case Sprite.KIND_GOOMBA_WINGED:
      case Sprite.KIND_ENEMY_FLOWER:
      case Sprite.KIND_RED_KOOPA:
      case Sprite.KIND_RED_KOOPA_WINGED:
      case Sprite.KIND_GREEN_KOOPA:
      case Sprite.KIND_GREEN_KOOPA_WINGED:
      case Sprite.KIND_BULLET_BILL:
        return true;
      default:
        return false;
    }
  }

  private int exitCreature() {
    int creature = 0;
    for (int r = 0; r < 19; r++) {
      for (int c = 0; c < 19; c++) {
        if (isCreature(enemies[r][c]) && !isObstacle(levelScene[r][c]))
          creature++;
      }
    }
    return creature;
  }

  private String nearMonstar(int r, int c) {
    for (int i = 1; i < 7; i++) {
      for (int j = 0; j < 19; j++) {
        if (isCreature(enemies[j][c + 2]) && !isObstacle(levelScene[j][c + 2])) {
          return "NEAR_IS_CREATURE";
        }
        if (!isObstacle(levelScene[j][c + i])) {
          switch (enemies[j][c + i]) {
            case Sprite.KIND_GOOMBA_WINGED:
              return "RIGHT_IS_KIND_GOOMBA_WINGED";
            case Sprite.KIND_RED_KOOPA:
            case Sprite.KIND_RED_KOOPA_WINGED:
            case Sprite.KIND_GREEN_KOOPA:
            case Sprite.KIND_GREEN_KOOPA_WINGED:
            case Sprite.KIND_BULLET_BILL:
              return "RIGHT_IS_MONSTAR";
            case Sprite.KIND_GOOMBA:
            case Sprite.KIND_ENEMY_FLOWER:
              return isObstacle(levelScene[j][c + i - 1]) || isObstacle(levelScene[j][c + i + 1])
                ? "RIGHT_IS_LIGHT_MONSTAR" : "RIGHT_IS_MONSTAR";
            default:
              break;
          }
          switch (enemies[j][c - i]) {
            case Sprite.KIND_GOOMBA_WINGED:
            case Sprite.KIND_RED_KOOPA:
            case Sprite.KIND_RED_KOOPA_WINGED:
            case Sprite.KIND_GREEN_KOOPA:
            case Sprite.KIND_GREEN_KOOPA_WINGED:
            case Sprite.KIND_GOOMBA:
              return exitCreature() == 1 && i != 1 ? "LEFT_IS_FEW_MONSTAR" : "LEFT_IS_MONSTAR";
            default:
              break;
          }
        }
      }
    }
    return "NONE";
  }

  private String nearItem(int r, int c) {
    for (int j = 0; j < 5; j++) {
      switch (levelScene[r - j][c]) {
        case GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH:
          return "NONE";
        case GeneralizerLevelScene.BRICK:
          return marioMode == 0 ? "NONE" : "ABOBE_IS_BRICK";
        case GeneralizerLevelScene.COIN_ANIM:
          return "ABOBE_IS_COIN_ANIM";
        default:
          break;
      }
      switch (enemies[r - j][c - 1]) {
        case Sprite.KIND_FIRE_FLOWER:
          return "LEFT_IS_FLOWER";
        default:
          break;
      }
    }
    return "NONE";
  }

  private int nearHole(int c) {
    int nearHoleIndex = 19;
    for (int i = 0; i < 3; i++) {
      nearHoleIndex = isHole(c - i) && nearHoleIndex == 19 ? -i : nearHoleIndex;
      nearHoleIndex = isHole(c + i) && nearHoleIndex == 19 ? i : nearHoleIndex;
    }
    return nearHoleIndex;
  }

  public void reset() {
    action = new boolean[Environment.numberOfKeys];
    action[Mario.KEY_RIGHT] = true;
  }

  /** 1tickごとに行動を決定します. */
  public boolean[] getAction() {
    final int r = marioEgoRow;
    final int c = marioEgoCol;

    action[Mario.KEY_JUMP] = (isObstacle(levelScene[r][c + 1]) || isObstacle(levelScene[r][c - 1]))
        && (isMarioAbleToJump || !isMarioOnGround);

    // まわりのアイテムを取りに行く
    switch (nearItem(r, c)) {
      case "ABOBE_IS_COIN_ANIM":
      case "ABOBE_IS_BRICK":
      case "ABOBE_IS_HIDDEN_ITEM":
        action[Mario.KEY_RIGHT] = isMarioAbleToJump && !isMarioOnGround;
        action[Mario.KEY_LEFT] = isMarioAbleToJump && !isMarioOnGround; // true
        action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
        break;
      case "LEFT_IS_FLOWER":
        action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
        action[Mario.KEY_RIGHT] = isMarioOnGround;
        action[Mario.KEY_LEFT] = !isMarioOnGround;
        break;
      default:
        action[Mario.KEY_RIGHT] = true;
        action[Mario.KEY_LEFT] = false;
        break;
    }

    // 周りの敵への処理
    switch (nearMonstar(r, c)) {
      case "RIGHT_IS_KIND_GOOMBA_WINGED":
      case "RIGHT_IS_MONSTAR":
        // 後退しながらファイアを打つ
        action[Mario.KEY_RIGHT] = isMarioAbleToShoot;
        action[Mario.KEY_LEFT] = true;
        action[Mario.KEY_SPEED] = isMarioAbleToShoot;
        break;
      case "RIGHT_IS_LIGHT_MONSTAR":
        // 前進しながらファイアを打つ
        action[Mario.KEY_LEFT] = false;
        action[Mario.KEY_SPEED] = isMarioAbleToShoot;
        action[Mario.KEY_RIGHT] = isObstacle(levelScene[r][c + 1])
             ? isMarioAbleToJump || !isMarioOnGround : isMarioOnGround;
        action[Mario.KEY_JUMP] = isObstacle(levelScene[r][c + 1])
             ? isMarioAbleToJump || !isMarioOnGround : isMarioOnGround;
        break;
      case "LEFT_IS_FEW_MONSTAR":
        // 左向きに打つ
        action[Mario.KEY_RIGHT] = false;
        action[Mario.KEY_LEFT] = isMarioAbleToShoot;
        action[Mario.KEY_SPEED] = isMarioAbleToShoot;
        break;
      default:
        action[Mario.KEY_SPEED] = false;
        break;
    }

    // 特定の障害物を飛び越える
    for (int i = 0; i < 6; i++) {
      if (isObstacle(levelScene[r + i][c + 1]) && isObstacle(levelScene[r - 3 + i][c + 1])
          && levelScene[r - 1 + i][c + 1] == 0) {
        action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
      }
    }

    // 穴に対する処理
    switch (nearHole(c)) {
      // 穴の上
      case 0:
        action[Mario.KEY_JUMP] = !isMarioOnGround;
        action[Mario.KEY_RIGHT] = true;
        action[Mario.KEY_LEFT] = false;
        break;
      // 穴の直前
      case 1:
        action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
        action[Mario.KEY_LEFT] = false;
        break;
      case -1:
        action[Mario.KEY_LEFT] = false;
        break;
      // 飛び過ぎないようにする
      case 2:
        action[Mario.KEY_LEFT] = isObstacle(levelScene[r][c + 1])
             ? isMarioOnGround : !isMarioOnGround;
        action[Mario.KEY_JUMP] = isObstacle(levelScene[r][c + 1])
             && (isMarioOnGround && isMarioAbleToJump);
        break;
      default:
        break;
    }
    return action;
  }
}
