package minsait.ttaa.datio.engine;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.jetbrains.annotations.NotNull;

import static minsait.ttaa.datio.common.Common.*;
import static minsait.ttaa.datio.common.naming.PlayerInput.*;
import static minsait.ttaa.datio.common.naming.PlayerOutput.*;
import static org.apache.spark.sql.functions.*;

public class Transformer extends Writer {
    private SparkSession spark;

    public Transformer(@NotNull SparkSession spark) {
        this.spark = spark;
        Dataset<Row> df = readInput();

        df.printSchema();

        //df = cleanData(df);
        //df = exampleWindowFunction(df);
        //df = columnSelection(df);

        df = functionQuestion23(df);
        df = filterData4(df);
        df = columnSelection(df);

        // for show 100 records after your transformations and show the Dataset schema
        df.show(100, false);
        df.printSchema();

        // Uncomment when you want write your final output
        write(df);
    }

    private Dataset<Row> columnSelection(Dataset<Row> df) {
        return df.select(
                shortName.column(),
                longName.column(),
                age.column(),
                heightCm.column(),
                weightKg.column(),
                nationality.column(),
                clubName.column(),
                overall.column(),
                potential.column(),
                teamPosition.column(),
                playerCat.column(),
                potential_vs_overall.column()
        );
    }

    /**
     * @return a Dataset readed from csv file
     */
    private Dataset<Row> readInput() {
        Dataset<Row> df = spark.read()
                .option(HEADER, true)
                .option(INFER_SCHEMA, true)
                .csv(INPUT_PATH);
        return df;
    }

    /**
     * @param df
     * @return a Dataset with filter transformation applied
     * column team_position != null && column short_name != null && column overall != null
     */
    private Dataset<Row> cleanData(Dataset<Row> df) {
        df = df.filter(
                teamPosition.column().isNotNull().and(
                        shortName.column().isNotNull()
                ).and(
                        overall.column().isNotNull()
                )
        );

        return df;
    }

    /**
     * @param df is a Dataset with players information (must have team_position and height_cm columns)
     * @return add to the Dataset the column "cat_height_by_position"
     * by each position value
     * cat A for if is in 20 players tallest
     * cat B for if is in 50 players tallest
     * cat C for the rest
     */
    private Dataset<Row> exampleWindowFunction(Dataset<Row> df) {
        WindowSpec w = Window
                .partitionBy(teamPosition.column())
                .orderBy(heightCm.column().desc());

        Column rank = rank().over(w);

        Column rule = when(rank.$less(10), "A")
                .when(rank.$less(50), "B")
                .otherwise("C");

        df = df.withColumn(catHeightByPosition.getName(), rule);

        return df;
    }

    private Dataset<Row> functionQuestion23(Dataset<Row> df) {
        WindowSpec w = Window
                .partitionBy(nationality.column())
                .partitionBy(teamPosition.column())
                .orderBy(overall.column().desc());

        Column rank = rank().over(w);

        Column rule = when(rank.$less(3), "A")
                .when(rank.$less(5), "B")
                .when(rank.$less(10), "C")
                .otherwise("D");

        df = df.withColumn(playerCat.getName(), rule);

        Column rule2 = potential.column().divide(overall.column());
        df = df.withColumn(potential_vs_overall.getName(), rule2);

        return df;
    }

    private Dataset<Row> filterData4(Dataset<Row> df) {
        df1 = df.filter(playerCat.column() == "A" || playerCat.column() == "B"));
        df2 = df.filter(playerCat.column() == "C" && potential_vs_overall.column()>1.15);
        df3 = df.filter(playerCat.column() == "D" && potential_vs_overall.column()>1.25);

        dfunion1 = df1.union(df2);
        dfunionall = dfunion1.union(df3);

        return dfunionall;
    }


}
