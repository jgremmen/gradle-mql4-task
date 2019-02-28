#property copyright "2019, Jeroen Gremmen"
#property strict

#include <Inc2.mqh>
#include "IncUnknown.mqh"
#include <>

#property indicator_chart_window
#property indicator_buffers 1
#property indicator_color1  Red

double ExtBuffer[];

int OnInit()
{
  return(INIT_SUCCEEDED);
}


int OnCalculate(const int rates_total,
                const int prev_calculated,
                const datetime &time[],
                const double &open[],
                const double &high[],
                const double &low[],
                const double &close[],
                const long& tick_volume[],
                const long& volume[],
                const int& spread[])
{
 return(rates_total);
}
