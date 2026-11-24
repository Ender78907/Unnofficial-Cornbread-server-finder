Find your next Minecraft server from the **Multiplayer** menu. Cornbread's Server Finder searches Cornbread2100's database, checks servers from your own connection, and lets you join a match with one click.

This is an **unofficial, independent project**. Cornbread2100 provides the data that makes discovery possible. The mod is unaffiliated with Cornbread2100, Mojang, and Fabric.

## Getting started

1. Choose the mod JAR that matches your **Minecraft Java Edition** version. Minecraft **26.3** needs Fabric Loader **0.19.5 or newer**. Minecraft **26.2** needs Fabric Loader **0.19.3 or newer**.
2. Use **Java 25** and install **Fabric API** for your Minecraft version.
3. Place the JAR in your instance's `mods` folder. This is a client-side mod. Only the player needs to install it.
4. Open **Multiplayer → Find servers** (**Buscar servers** in Spanish).

**Updating?** Replace the old JAR so only one version is installed. Keep `config/cornbread-finder.json` to preserve your settings and lists.

## Find and join a server

Set a minimum and maximum number of online players, or leave the maximum blank for no upper limit. You can also filter by whitelist status, cracked/offline mode, and compatible protocol. If a server's whitelist or cracked status is unknown, it appears only when the corresponding filter is set to **Any**.

- **Search** looks for up to 40 unique, verified matches. **More results** looks for another batch. Both keep checking pages until they reach their target or run out of candidates.
- **Search & join** connects to the first verified match using its fresh status response and cancels the remaining checks. Selecting a server manually checks its status again before connecting.
- **Refresh** rechecks up to 200 saved servers in the selected list. Blocked servers are skipped. Up to 12 status checks can run at once.

You can cancel a search at any time. The finder skips offline servers and retries timed-out queries with increasing delays, keeping its progress. An access denial or rate limit stops the search.

## Save and organize servers

- **Favorites** keeps servers you star from the **…** menu, even when your search results change.
- **Edit alias** gives an address a local name of up to 48 characters across all finder lists. Aliases are saved per IP and port. The join tooltip and server options still show the real address. Clear the alias to display the address again.
- **…** also lets you copy the IP and port or add the server to Minecraft's regular Multiplayer list. Saving the same address twice does not create a duplicate.
- **Found** keeps results and their last-check dates between menu visits and game restarts. Opening the finder leaves the list as you saved it. Use **Refresh** to see which servers are still online, or **Search** to find new ones.

## History and failed connections

**History** counts successful joins on your device, including joins through **Search & join**. Joining a known saved address through Minecraft's regular Multiplayer list counts too. **Failed** records unsuccessful attempts separately, with the number of attempts, the date, and the last error. Failed attempts, cancellations, and voluntary departures never increase the successful-join count.

**Found**, **History**, and **Failed** each hold up to 200 servers. Favorites are stored separately. History carried over from an older version may show a minimum such as **≥1** if earlier visits were never counted.

## Skip or block a server

- **In the finder**, click the square **×** immediately left of **…**, or choose the block option under **…**. Blocking hides the server from results, history, failed attempts, and favorites, and skips future status checks. Restore it from **Blocked**. Its alias and favorite status are preserved, as is any entry in Minecraft's regular Multiplayer list.
- **After a failed connection from the finder**, the error screen keeps Minecraft's original error details. **Find another server** temporarily skips that address. **Block server** hides it until you unblock it. **Block & find another** does both in one step.
- **While playing multiplayer**, choose **Leave & find** in the pause menu to disconnect normally and find the first match for your saved filters. This skips the server you just left without blocking it. The usual disconnect button is also available.

## Layout, language, and player lists

Choose **Normal**, **Compact**, or **List** view. List view hides the filters to make room for more servers. The layout adapts to the window size and uses Minecraft's buttons and backgrounds, with server icons, player counts, pages, and progress indicators.

Open **… → Online players** to see the names a server includes in its public status sample. Servers can customize this sample or leave players out. **Past players** combines Cornbread's public records with names observed locally and shows last-seen dates for up to 500 names per server. A past player may still be online.

Switch between **English** and **Español** independently of Minecraft's language. Server descriptions stay in the language their operators wrote them in. The finder saves your language, view, filters, aliases, favorites, blocks, and history locally in `config/cornbread-finder.json`.

## Connection notes

**Using a VPN?** Server discovery and status checks may not work while your VPN is active.

Discovery depends on the availability of Cornbread2100's API. The finder contacts that API for candidates and sends standard Minecraft server-list status requests to those servers from your connection.

Whitelist and cracked/offline-mode records can be outdated. Servers report their own player information. The compatibility filter compares advertised protocol versions, so a proxy may accept more versions than the finder shows. An online server can still reject a connection because of its whitelist, a ban, capacity, version, or other entry requirements. The mod respects those restrictions and leaves your account authentication unchanged.
