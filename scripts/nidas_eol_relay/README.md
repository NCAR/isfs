# Deploying nidas_udp_relay on eol-rt-data

> **WARNING**: NIDAS has no way to detect which samples are associated with
> which projects. If a DSM from one project feeds samples to a relay expecting
> a different project, the samples are merged into the output stream as if
> from the same project, confusing the sample streams or irreparably
> corrupting the data. Make sure that a relay instance (port number) will
> never be used by DSMs from a different project.  Do not switch projects for
> a relay instance unless all DSMs are known to be decommissioned. Probably
> the best practice is to cycle through ports from one project to the next to
> lessen the chance of collisions.  Yes, this needs to be fixed.

The `nidas_udp_relay` program receives sample data from multiple DSMs running
somewhere on the Internet, usually for a field project.  Each instance runs
for a single project.  The relay inserts a project-specific sample header to
the combined sample stream output, and NIDAS sample streams can only handle
samples for a single project.  Therefore, each instance uses different ports.
Usually each instance uses the same port number, receiving samples with a UDP
port and sending samples over a TCP port.

For the **CentOS 8** version of `eol-rt-data`, the `nidas_udp_relay` process
is managed by an init script `check_nidas_udp_relay.sh`, and that script is
called from a `crontab` file.  That script works like a typical init script,
and in fact it requires `/etc/init.d/functions` to be installed from the
`initscripts` package.  Since other processes (EOL weather stations in
particular) run from `cron` under the `isfs` account, the relay portion of the
`crontab` is stored in `eol_relay.crontab`.  Then the `set_crontab.sh` script
can be used to install the relay portion and the weather station portion into
a single `crontab`.

On the **Alma 10** OS, the `nidas_udp_relay` process can be managed as a
`systemd` user unit, so it can be managed completely separately from the
weather station processes.  The service file is a template which can be
instantiated for each of the ports allocated to the relay, 30009-30012. Then
the project-specific header path can be changed by adding a drop-in override
file which overrides the `HEADER` setting:

```ini
Environment=HEADER="-h %h/isfs/projects/MARSHALL2023/ISFS/config/marshall2023-header.txt"
```

Below are the general steps to deploying `nidas_udp_relay` to the Alma 10
version of `eol-rt-data`. Note that `linger` must be enabled for the `isfs`
user so the service can start without a login.

```sh
cd ~isfs
git clone git@github.com:/NCAR/isfs
cd isfs
git submodule update --init
cd projects/
git checkout master
git pull
cd ~isfs/scripts/nidas_eol_relay
systemctl --user link `realpath ./nidas_udp_relay@.service`
# make sure an instance exists for port 30010
systemctl --user enable nidas_udp_relay@30010
# add a drop-in override for the HEADER line
systemctl --user edit nidas_udp_relay@30010
# start the service and show the status
systemctl --user start nidas_udp_relay@30010
systemctl --user status nidas_udp_relay@30010
```

The full service unit, complete with the drop-in override, can be shown with
this command:

```sh
systemctl --user cat nidas_udp_relay@30010
```
