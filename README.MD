<b>Due to the Log4j security vulnerabillity, any Starloader-launcher version below 3.0.0 should not be used with this mod!
I am not kidding, I have seen this vulnerabillity being used across a few channels.</b>
Starloader-launcher 3.+ is safe to use as it uses logback instead of Log4j.

# Galimulator IRC

A galimulator mod that adds a basic, yet powerfull IRC Client.
The goal of this client is to be the IRC Client of my dreams

## Existing features

- Multi-network and multi-channel capabillity
- Easily join networks and channels via /connect and /join.
- Per-network SASL and Nickserv integration (not tested though)
- Desktop notifications via Notify-send (usually only comes packaged on linux systems)
- Channel monitoring

## Planned features

- CTCP sending and displaying
- Automatically detect bridges and remap usernames
- Detect hyperlinks and make them clickable
- Privmessages
- Support /me
- "spontanousMonitor", which sends notification from time to time, but not always like it is the case with "monitor"
- fix issues with KICL not being able to parse user modes
- log to file
- automatically request backlog from irssiproxy, if existing
- send monitored messages in galimulator's bulletin log
- leaving networks and channels

## Screenshots

<img src="https://files.geolykt.de/GalimIRC-example-1.png" />
<img src="https://files.geolykt.de/GalimIRC-example-2.png" />
<img src="https://files.geolykt.de/GalimIRC-example-3.png" />
