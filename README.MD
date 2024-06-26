# Galimulator IRC

A galimulator mod that adds a basic, yet powerful IRC Client.
The goal of this client is to be the IRC Client of my dreams

## Existing features

- Multi-network and multi-channel capability
- Easily join networks and channels via /connect and /join.
- Per-network SASL and NickServ integration (not tested though)
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

# Minimum requirements

- SLL (any version and flavour that can run the other dependencies)
- SLAPI 2.0.0-a20240524.1 or higher (might run on older versions too, but hasn't been tested to do so)
- Java 8

## Screenshots

<img src="https://geolykt.de/files/GalimIRC-example-1.png" />
<img src="https://geolykt.de/files/GalimIRC-example-2.png" />
<img src="https://geolykt.de/files/GalimIRC-example-3.png" />
