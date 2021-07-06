# Spigot Chunk Protector
by Gabriel Tofvesson (IKEA_Jesus)

## Index

* [Permissions](#Permissions)

* [Commands](#Commands)

    * [claim](#claim)

    * [unclaim](#unclaim)

    * [claims](#claims)

    * [invite](#invite)

    * [uninvite](#uninvite)

    * [showclaim](#showclaim)

    * [claimowner](#claimowner)

    * [claimoption](#claimoption)

        * [Options](#Options)

## Permissions

|Permission Node|Description|Applies to (by default)|
|:---|:---|:---:|
|`chunkprotector.claim`|Allows access to [`/claim`](#claim)|All players|
|`chunkprotector.unclaim`|Allows access to [`/unclaim`](#unclaim)|All players|
|`chunkprotector.invite`|Allows access to [`/invite`](#invite)|All players|
|`chunkprotector.uninvite`|Allows access to [`/uninvite`](#uninvite)|All players|
|`chunkprotector.listclaims`|Allows access to [`/claims`](#claims)|All players|
|`chunkprotector.claimowner`|Allows access to [`/claimowner`](#claimowner)|All players|
|`chunkprotector.showclaim`|Allows access to [`/showclaim`](#showclaim)|All players|
|`chunkprotector.claimoption`|Allows access to [`/claimoption`](#claimoption)|All players|
|`chunkprotector.bypass`|Allows for modification of otherp player's claims|Operators|
|`chunkprotector.ignore`|Allows bypassing of all physical restrictions|Operators|

## Commands

### claim

*Claim an area and give it a specified name*

| Action |Command|
|:--- | :---: |
|Select area|`/claim [name]`|
|Cancel selection|`/claim`|


### unclaim

*Remove a claim with a given name for a specific player*

|Who|Command|
|:--- | :---:|
|All players|`/unclaim [claim]`|
|Bypass permission + Console|`/unclaim [claim] [player]`|


### claims

*List all claimed areas for a player*

|Who|Command|
|:--- | :---:|
|All players|`/claims`|
|Bypass permission + Console|`/claims [player]`|


### invite

*Invite a player to a claim*

|Who|Command|Context|
|:--- | :---:|:---|
|All players|`/invite [player]`|Invite player to claim that owner is standing in|
|All players|`/invite [player] [claim]`|Invite player to given claim|
|Bypass permission + Console|`/invite [player] [claim] [claimOwner]`|Invite player to another players claim|


### uninvite

*Un-invite a player to a claim*

|Who|Command|Context|
|:--- | :---:|:---|
|All players|`/uninvite [player]`|Un-invite player to claim that owner is standing in|
|All players|`/uninvite [player] [claim]`|Un-invite player to given claim|
|Bypass permission + Console|`/uninvite [player] [claim] [claimOwner]`|Un-invite player to another players claim|


### showclaim

*Show the boundaries of a claimed area*

|Action|Command|
|:--- | :---:|
|Current claim|`/claims`|
|Specific claim|`/claims [claim]`|
|Specific claim|`/claims [claim] [player]`|


### claimowner

*Get the name and owner of the claim the command sender is standing in*

|Where|Command|
|:--- | :---:|
|Current claim|`/claimowner`|


### claimoption

*Set a configuration option for a given claim. Values will always be either `true` or `false`*

|Who|Command|Context|
|:--- | :---:|:---|
|All players|`/claimoption [option] [value]`|Set option for the claim the command sender is standing in|
|All players|`/claimoption [claim] [option] [value]`|Set option for the given claim|
|Bypass permission + Console|`/claimoption [player] [claim] [option] [value]`|Set option for the claim of the given player|


### Options

|Option name|Description|
|:---|:---|
|`allowAllLiquids`|Allow all liquids to enter a claimed area|
|`allowEntityInteract`|Allow entities to interact with the environment (e.g. creepers break blocks, mobs can hurt each other). When this is false, mobs will act as if players that are not invited to a claim do not exist|
|`allowGuestLiquids`|Allow liquids to enter claimed area if they originate from a claim owned by an invited player|
|`allowPlayerEntityInteract`|Allow all players (invited or not) to interact with non-hostile mobs in a claim|
|`allowTNT`|Allow TNT explosions to break blocks|
|`disablePVP`|Disable player-versus-player interactions|

**NOTE:** Regarding `allowPlayerEntityInteract`, players can always interact with hostile mobs, so long as said mobs do not have a custom name.