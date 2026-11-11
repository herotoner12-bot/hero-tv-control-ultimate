const COMMANDS = {
  up: 19,
  down: 20,
  left: 21,
  right: 22,
  select: 23,
  back: 4,
  home: 3,
  menu: 82,
  volume_up: 24,
  volume_down: 25,
  mute: 164,
  play_pause: 85
};

function getCommand(name) {
  if (!Object.hasOwn(COMMANDS, name)) {
    throw new Error('Command not allowed');
  }
  return COMMANDS[name];
}

module.exports = { COMMANDS, getCommand };
