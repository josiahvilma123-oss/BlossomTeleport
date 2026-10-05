# ✿ BlossomTeleport

Custom teleport plugin for Blossom SMP (Paper 1.21.11). No other plugins needed.

## Commands
| Command | What it does |
|---|---|
| /tpa <player> | Ask to teleport to someone |
| /tpahere <player> | Ask someone to come to you |
| /tpaccept, /tpdeny | Accept or deny (or click the buttons in chat) |
| /tpacancel | Cancel a request you sent |
| /tptoggle | Turn requests on/off |
| /rtp (/wild) | Random teleport into the wild |
| /sethome [name], /home [name], /delhome <name> | Homes |
| /homes | Homes menu (click to teleport, shift+right-click to delete) |
| /spawn | Go to spawn |
| /setspawn | Set spawn (admin) |
| /back | Go back to your last spot or where you died |
| /blossomtp reload | Reload the config (admin) |

## Extra homes
`lp group <group> permission set blossomteleport.homes.staff true` (uses `homes.limits` in config)
or a number: `blossomteleport.homes.10`. Unlimited: `blossomteleport.homes.unlimited`.

## Build
GitHub Actions runs `mvn -B package` and uploads `target/BlossomTeleport.jar`.
