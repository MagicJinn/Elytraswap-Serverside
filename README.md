# Elytraswap Serverside

![icon.png](https://raw.githubusercontent.com/MagicJinn/Elytraswap-Serverside/refs/heads/main/icon.png)

## Automatic elytra and chestplate swapping, entirely on the server

Equip your chestplate while on the ground and your elytra while in the air, without a client mod. **Elytraswap Serverside** swaps between the best elytra and chestplate in your inventory when you take off or land, so every player on the server gets the same seamless flight experience.

Swap scoring and trigger rules are a port of [JJElytraSwap](https://github.com/JumperOnJava/JJElytraSwap) (MIT).

### Features

- **Automatic Swapping:** Equips your best elytra when you take off, and your best chestplate when you land.
- **Smart Selection:** Prefers higher armor, toughness, Protection, Mending, Unbreaking, and named chestplates. Elytras are scored by Mending and Unbreaking.
- **Serverside:** When installed on a server, players do not need to install the mod. Can also be installed in singleplayer worlds.
- **Per-Player Opt-Out:** Players can disable or re-enable swapping for themselves with a command.
- **Safety Checks:** Respects Curse of Binding on chestplates and elytras, skips broken elytras, skips empty chest slots, and stays out of creative and spectator mode.

### How it works

While you are airborne and still wearing a chestplate, the mod rewrites your equipment packets so the client sees that chestplate with a temporary **glider** component. That lets takeoff be detected and started without swapping server-side yet. Your player will not switch until a swap is needed (for example when you actually take off or land), and to other players it will appear and act like you're still wearing your chestplate.

### Limitations

- **Creative mode is not supported.** Creative players have full authority over their inventory, so the mod stays inactive there to avoid sync conflicts that could duplicate or delete items.
- **Clientside is not supported.** This mod will do nothing when installed on your client, but not on the server. Singleplayer is supported.

### Commands

| Command              | Description                              |
| -------------------- | ---------------------------------------- |
| `/elytraswap`        | Show whether swapping is enabled for you |
| `/elytraswap toggle` | Opt yourself out or back in              |
