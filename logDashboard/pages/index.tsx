import { ReactElement, useState } from 'react';
import { ResponsivePie } from '@nivo/pie';
import { ResponsiveLine, Serie } from '@nivo/line';
import {
  append,
  filter,
  findFirst,
  head,
  last,
  map,
  modifyAt,
  reduce,
} from 'fp-ts/Array';
import * as O from 'fp-ts/Option';
import { LogData } from 'app-types';
import { LogLevel, logLevelColors, logLevelList } from '../types/log-types';
import { GetServerSidePropsResult } from 'next';
import axios from 'axios';
import { pipe } from 'fp-ts/lib/function';

const sData = (log_id: string, loglevel: LogLevel, timestamp: string) => ({
  log_id,
  timestamp,
  loglevel,
  log_message: 'some message',

});

const data: LogData[] = [

];

interface LogPieChart {
  id: LogLevel;
  label: LogLevel;
  value: number;
  color: string;
}

const dataToPieChart = (data: LogData[]): LogPieChart[] =>
  pipe(
    data,
    reduce<LogData, LogPieChart[]>([], (acc, g) =>
      pipe(
        acc,
        findFirst((d) => d.id === g.log_type),
        O.fold(
          () =>
            pipe(
              acc,
              append({
                id: g.log_type,
                label: g.log_type,
                color: logLevelColors[g.log_type],
                value: 1,
              })
            ),
          () =>
            pipe(
              acc,
              map((d) =>
                d.id === g.log_type
                  ? {
                      ...d,
                      value: d.value + 1,
                    }
                  : d
              )
            )
        )
      )
    )
  );

interface LogLineChart extends Serie {
  id: LogLevel;
  color: string;
}

const groupLog = (
  source: LogData[][],
  loglevel: LogLevel
): { x: string; y: number }[] =>
  pipe(
    source,
    map((l) =>
      pipe(
        l,
        filter((dl) => dl.log_type === loglevel)
      )
    ),
    reduce<LogData[], { ind: number; dd: { x: string; y: number }[] }>(
      {
        ind: 0,
        dd: [],
      },
      (acc, d) => ({
        ind: acc.ind + 1,
        dd: pipe(
          acc.dd,
          append({
            x: pipe(
              d,
              head,
              O.fold(
                () => `${acc.ind}`,
                () => `${acc.ind}`
              )
            ),
            y: d.length,
          })
        ),
      })
    )
  ).dd;

interface PropsData {
  overallData: LogData[];
  pieData: LogPieChart[];
  lineData: LogLineChart[];
  sliding: LogData[][];
}

const slidingWindow = (data: LogData[]): LogData[][] =>
  pipe(
    data,
    reduce<LogData, { untilDate: O.Option<Date>; groups: LogData[][] }>(
      {
        groups: [],
        untilDate: O.none,
      },
      (acc, e) =>
        pipe(
          acc.untilDate,
          O.fold(
            // No window date
            () => {
              console.log(new Date(e.timestamp));
              console.log(
                new Date(new Date(e.timestamp).getTime() + 1 /*minute*/ * 60000)
              );
              return {
                untilDate: O.some(
                  new Date(new Date(e.timestamp).getTime() + 1 /*minute*/ * 60000)
                ),
                groups: pipe(
                  acc.groups,
                  last,
                  O.fold(
                    () => [[e]],
                    () =>
                      pipe(
                        acc.groups,
                        modifyAt(acc.groups.length - 1, (l) =>
                          pipe(l, append(e))
                        ),
                        O.fold(
                          () => {
                            console.error('No last');
                            return acc.groups;
                          },
                          (lst) => lst
                        )
                      )
                  )
                ),
              };
            },
            // Window date found
            (date) =>
              pipe(
                date,
                O.fromPredicate((dd) => new Date(e.timestamp) <= dd),
                O.fold(
                  // New window starts
                  () => ({
                    untilDate: O.some(
                      new Date(
                        new Date(e.timestamp).getTime() + 1 /*minute*/ * 60000
                      )
                    ),
                    groups: pipe(acc.groups, append([e])),
                  }),
                  // Add to previous window
                  () => ({
                    untilDate: acc.untilDate,
                    groups: pipe(
                      acc.groups,
                      last,
                      O.fold(
                        () => [[e]],
                        () =>
                          pipe(
                            acc.groups,
                            modifyAt(acc.groups.length - 1, (l) =>
                              pipe(l, append(e))
                            ),
                            O.fold(
                              () => {
                                console.error('No last');
                                return acc.groups;
                              },
                              (lst) => lst
                            )
                          )
                      )
                    ),
                  })
                )
              )
          )
        )
    )
  ).groups;

export async function getServerSideProps(): Promise<
  // context: GetServerSidePropsContext
  GetServerSidePropsResult<PropsData>
> {
  try {
    const { data: resData } = await axios.get<LogData[]>(
      'http://localhost:3000/api/get-data'
    );

    return {
      props: {
        overallData: resData,
        sliding: slidingWindow(resData),
        lineData: logLevelList.map((ll) => ({
          id: ll,
          color: logLevelColors[ll],
          data: groupLog(slidingWindow(resData), ll),
        })),
        pieData: dataToPieChart(resData),
      },
    };
  } catch (_) {
    return {
      props: {
        overallData: data,
        sliding: slidingWindow(data),
        lineData: logLevelList.map((ll) => ({
          id: ll,
          color: logLevelColors[ll],
          data: groupLog(slidingWindow(data), ll),
        })),
        pieData: dataToPieChart(data),
      },
    };
  }
}

export default function Index(props: PropsData): ReactElement {
  console.log({ props });

  const [logLevelFilter, setLogLevelFilter] = useState<LogLevel[]>([]);
  const filterAllLogLevel = props.overallData.filter(
    (d) => logLevelFilter.length === 0 || logLevelFilter.includes(d.log_type)
  );
  const filterByLogLevel = props.pieData.filter(
    (d) => logLevelFilter.length === 0 || logLevelFilter.includes(d.label)
  );
  const filterLineLogLevel = props.lineData.filter(
    (d) => logLevelFilter.length === 0 || logLevelFilter.includes(d.id)
  );
  const toggleFilter = (l: LogLevel) => {
    if (logLevelFilter.includes(l)) {
      setLogLevelFilter(logLevelFilter.filter((f) => f !== l));
    } else {
      setLogLevelFilter([...logLevelFilter, l]);
    }
  };
  return (
    <div>
      <div className="w-full min-h-screen p-20 bg-blue-50">
        <div className="flex items-center justify-center gap-6">
          {logLevelList.map((ll) => (
            <button
              key={ll}
              type="button"
              onClick={() => toggleFilter(ll)}
              className={`${
                logLevelFilter.includes(ll)
                  ? 'underline font-bold'
                  : 'font-light'
              }`}
            >
              {ll}
            </button>
          ))}
        </div>
        <div className="h-[20rem] flex justify-evenly">
          <MyResponsivePie data={filterByLogLevel} />
          <MyResponsiveLine data={filterLineLogLevel} />
        </div>
        <div className="flex flex-col mt-20">
          <div className="-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
            <div className="inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8">
              <div className="overflow-hidden border border-gray-400 shadow-md sm:rounded-lg">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th
                        scope="col"
                        className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase"
                      >
                        Timestamp
                      </th>
                      <th
                        scope="col"
                        className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase"
                      >
                        Loglevel
                      </th>
        
                      <th
                        scope="col"
                        className="px-6 py-3 text-xs font-medium tracking-wider text-left text-gray-500 uppercase"
                      >
                        Message
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {filterAllLogLevel.map((log, logIndex) => (
                      <tr
                        key={logIndex}
                        className={
                          logIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'
                        }
                      >
                        <td className="px-6 py-4 text-sm font-medium text-gray-900 whitespace-nowrap">
                          {log.timestamp}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                          <span
                            className="px-3 py-1 text-sm font-bold text-white rounded-md shadow-inner"
                            style={{
                              backgroundColor: logLevelColors[log.log_type],
                            }}
                          >
                            {log.log_type}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                          {log.log_message}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

const MyResponsivePie = ({ data }: { data: LogPieChart[] }) => (
  <ResponsivePie
    data={data}
    margin={{ top: 80, right: 80, bottom: 20, left: 80 }}
    // innerRadius={0.5}
    // padAngle={0.7}
    // cornerRadius={3}
    activeOuterRadiusOffset={8}
    borderWidth={4}
    borderColor={{ from: 'color', modifiers: [['darker', 0.2]] }}
    // arcLinkLabelsSkipAngle={10}
    // arcLinkLabelsTextColor="#333333"
    arcLinkLabelsThickness={10}
    arcLinkLabelsColor={{ from: 'color' }}
    // arcLabelsSkipAngle={10}
    arcLabelsTextColor={{ from: 'color', modifiers: [['darker', 2]] }}
    colors={{ datum: 'data.color' }}
    legends={[
      {
        anchor: 'top',
        direction: 'row',
        justify: false,
        translateX: 0,
        translateY: -56,
        itemsSpacing: 0,
        itemWidth: 100,
        itemHeight: 18,
        itemTextColor: '#999',
        itemDirection: 'left-to-right',
        itemOpacity: 1,
        symbolSize: 18,
        symbolShape: 'circle',
        effects: [
          {
            on: 'hover',
            style: {
              itemTextColor: '#000',
            },
          },
        ],
      },
    ]}
  />
);

const MyResponsiveLine = ({ data }: { data: LogLineChart[] }) => (
  <ResponsiveLine
    data={data}
    margin={{ top: 50, right: 110, bottom: 50, left: 60 }}
    xScale={{ type: 'point' }}
    yScale={{
      type: 'linear',
      min: 'auto',
      max: 'auto',
      // stacked: true,
      reverse: false,
    }}
    yFormat=" >-.2f"
    enableArea={true}
    areaOpacity={0.25}
    colors={Object.values(logLevelColors)}
    axisTop={null}
    axisRight={null}
    axisBottom={{
      // orient: 'bottom',
      tickSize: 5,
      tickPadding: 5,
      tickRotation: 0,
      legend: 'time',
      legendOffset: 36,
      legendPosition: 'middle',
    }}
    axisLeft={{
      // orient: 'left',
      tickSize: 5,
      tickPadding: 5,
      tickRotation: 0,
      legend: 'number of logs',
      legendOffset: -40,
      legendPosition: 'middle',
    }}
    pointSize={10}
    pointColor={{ theme: 'background' }}
    pointBorderWidth={2}
    pointBorderColor={{ from: 'serieColor' }}
    // pointLabelYOffset={-12}
    useMesh={true}
    legends={[
      {
        anchor: 'bottom-right',
        direction: 'column',
        justify: false,
        translateX: 100,
        translateY: 0,
        itemsSpacing: 0,
        itemDirection: 'left-to-right',
        itemWidth: 80,
        itemHeight: 20,
        itemOpacity: 0.75,
        symbolSize: 12,
        symbolShape: 'circle',
        symbolBorderColor: 'rgba(0, 0, 0, .5)',
        effects: [
          {
            on: 'hover',
            style: {
              itemBackground: 'rgba(0, 0, 0, .03)',
              itemOpacity: 1,
            },
          },
        ],
      },
    ]}
  />
);
