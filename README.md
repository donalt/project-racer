# Racer Project

Racer Project is as one might expect a racing game. The racing is done on tracks that have been generated from manually written files. Because of there being no real editor, the tracks are pretty simple. There's no mud, grass or water but plenty of tricky turns. For better or worse there are also items that help you on your journey to the finish line. Some items powers up your car while you can use others as projectiles and traps.

Local multiplayer is possible with up to 4 players sharing a single screen. The biggest problem here is keyboard space and also how many buttons can be pressed simultaneously on your keyboard.

## Testing

We will not be able to test if the graphics work as they should, except for if they crash the game or not. Therefore "manual" testing for that will suffice. Instead, all of our unit testing will be on the actual game code that judges collisions, behaviour of cars, procedural generation and so on.

## Installing

* Linux or Mac
  * Run `gradlew`

* Windows
  * Run `gradlew.bat`

If the compiler complains that it cannot find a `.png`, copy the contents of `core/assets/` into `desktop/bin/`

Alternatively if you just want to play the game, you can use `core/project-racer.jar`.

## Controls
The default controls are as follows:

Player | Accelerate | Brake | Turn Left | Turn Right | Use Item | Look Back
------ | ---------- | ----- | --------- | ---------- | -------- | ---------
1 | W | S | A | D | Left Shift | Left Ctrl
2 | UP | DOWN | LEFT | RIGHT | Right Shift | Right Ctrl
3 | I | K | J | L | B | N
4 | 8 | 5 | 4 | 6 | 0 | 1

*The controls for player 4 refer to the numpad.

As mentioned before, [most keyboards aren't wired for them to able to register many keys at the same time](https://en.wikipedia.org/wiki/Rollover_(key)). Therefore, more than 2 players is not recommended unless you have a super awesome keyboard.
