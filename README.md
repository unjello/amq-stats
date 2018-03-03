# amq-stats

- amq-stats is a tiny cli tool for dumping ActiveMQ statistics from JMX to stdout.

## How does it work?

It connects to `localhost` on a given port, via JMX and prints out selected stats in CSV format.

## Usage

```bash
$ amq-stats 1099
```

## License

- Unlicense (~Public Domain)