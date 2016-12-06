Stock-trader
=================================
Stock backtester using Yahoo Finance EOD data.

Using Scala, Postgres, Quill.

To run:  
Fill out env vars.   
`sbt download stock`   
`sbt download userLists`   
`sbt run backtest run --from 2000 --to 5/17/2017`   

Env Vars:   
`DATABASE_USER`  
`DATABASE_PASSWORD`  
`DATABASE_NAME`  
`DATABASE_URL`  
`DATABASE_PORT`   
`CHART_VIEWER_URL`   

![](https://cloud.githubusercontent.com/assets/2387719/20647544/95625d68-b44b-11e6-91df-2a60daa394d4.png)
