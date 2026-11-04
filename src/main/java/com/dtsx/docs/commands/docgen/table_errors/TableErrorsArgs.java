package com.dtsx.docs.commands.docgen.table_errors;

import com.dtsx.docs.config.args.BaseArgs;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ToString
public class TableErrorsArgs extends BaseArgs<TableErrorsCtx> {
    @Parameters(
        description = "Input YAML file or URL",
        paramLabel = "INPUT_FILE"
    )
    public String $input;

    @Option(
        names = { "-o", "--output" },
        description = "Output AsciiDoc file",
        paramLabel = "OUTPUT_FILE",
        defaultValue = "../docs-common/modules/data-api/partials/dataapi-errors.adoc"
    )
    public String $output;

    @Override
    public TableErrorsCtx toCtx(CommandSpec spec) {
        return new TableErrorsCtx(this, spec);
    }
}
