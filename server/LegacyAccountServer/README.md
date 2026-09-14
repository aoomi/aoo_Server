# LegacyAccountServer

The 2.22 account/hall implementation is retained outside the production Maven
reactor for behavioral and data-format comparison. New identity, session and
profile contracts are owned by `server/Account`; asset mutation is owned solely
by `server/Billing`. No modern module may depend on this directory.
