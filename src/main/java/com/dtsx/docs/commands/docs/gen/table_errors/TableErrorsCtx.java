package com.dtsx.docs.commands.docs.gen.table_errors;

import com.dtsx.docs.config.ctx.BaseCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@Getter
public class TableErrorsCtx extends BaseCtx {
    private final String input;
    private final String output;

    public TableErrorsCtx(TableErrorsArgs args, CommandSpec spec) {
        super(args, spec);
        this.input = args.$input;
        this.output = args.$output;
    }
}
