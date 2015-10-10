Temporal Filtering in SensorHub SOS
---


SensorHub's SOS implementation supports both **historical** and **real-time** requests for sensor data.

Historical requests are automatically enabled (and properly advertised in the capabilities) when a storage is configured and associated to a sensor.

Likewise, real-time requests are automatically enabled when a sensor is directly connected to the SOS (and the sensor module is itself enabled and reports the sensor as connected).

Regarding temporal filtering, SensorHub's implementation only supports the **TEquals** operator for time instants and **During** operator for time periods and, currently, filtering can only be done on the 'phenomenonTime' property. The special value 'now' represents the limit between historical and real-time data.

The following tables explain how the server responds to different temporal filter settings:


### Time Instants
| **Behavior** | **Request** |
|:-------------|:------------|
| Get observation at exact time | `temporalFilter=phenomenonTime,2014-04-01T00:00:00Z` |
| Get latest observation        | `temporalFilter=phenomenonTime,now`                  |


### Time Periods
*In all the examples below, 'now' is considered to be at 2014-02-20 and we assume storage contains data until this date.*

| **Behavior** | **Request** |
|:-------------|:------------|
| Get observations for a historical time range      | `temporalFilter=phenomenonTime,2014-01-01/2014-02-01` |
| Get historical data up to the latest observation  | `temporalFilter=phenomenonTime,2014-01-01/now`        |
| Get real-time stream ending at specific time (1,2)  | `temporalFilter=phenomenonTime,now/2014-03-01`      |
| Get observations for a time range overlapping past and future (1,3) (*not supported for now*) | `temporalFilter=time,2014-01-01/2014-03-01` |

(1) A real-time stream is only available through a persistent connection if sampling period is lower than a certain threshold (usually a few seconds). For lower rate data producers, the subscription service can be used (**TODO:** implement SOS subscription service)

(2) The stream will be closed as soon as an observation more recent than the end date is produced. A date very far in the future can be used to get a virtually never ending data stream.

(3) If real-time streaming is available, the stream will be closed as in (2). Otherwise it will be closed right after the latest available observation has been sent, even if it is much earlier than the end date (i.e. the request will be treated as `temporalFilter=time,2014-01-01/now`).

*Note: Dates used are in the ISO8601 format and can include the time part or not (e.g. both 2014-01-01Z and 2013-05-06T12:05:00.111Z are valid). When no time is specified, midnight (00:00:00) is assumed.*


### Replay Extension
The SOS implementation also supports replaying historical observations at arbitrary speed (i.e. at real-time speed, or slower/faster than real-time). This is supported by adding the `replaySpeed` parameter to a historical GetResult KVP request, such as in `temporalFilter=phenomenonTime,2014-01-01/2014-02-01&replaySpeed=2`. The parameter has no effect for a live stream request (i.e. if the requested period starts at 'now')

Observations are replayed at exactly real-time speed (according to observations time tags) with `replaySpeed=1`. The replay value is in fact a factor relative to real-time so that 10 means replaying 10x faster than real-time (if bandwidth permits!) and 0.1 means replaying 10x slower than real-time.

SensorHub implements this functionality by pausing the SOS data provider thread just the right amount of time to match the period infered by two successive measurement time stamps.

