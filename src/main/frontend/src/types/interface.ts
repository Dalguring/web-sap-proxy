export interface FieldMapping {
    webField: string;
    sapField: string;
    type?: string;
    required?: boolean;
    size?: number;
    remarks?: string;
    defaultValue?: string;
    example?: string;
}

export interface ImportMapping {
    webField: string;
    sapField: string;
    type?: string;
    required?: boolean;
    size?: number;
    remarks?: string;
    defaultValue?: string;
    example?: string;
}

export interface TableMapping {
    webFields: string;
    sapTable: string;
    singleValue: boolean;
    required: boolean;
    fields: FieldMapping[];
}

export interface ExportMapping {
    sapParam: string;
    webField: string;
    type?: string;
    size?: number;
    remarks?: string;
    example?: string;
}

export interface ReturnTableMapping {
    sapReturnTable: string;
    webReturnList: string;
    fields: FieldMapping[];
}

export interface InterfaceDefinition {
    id: string;
    name: string;
    description: string;
    sapModule: string;
    rfcFunction: string;
    executable: boolean;
    importMapping: ImportMapping[];
    tableMapping: TableMapping[];
    exportMapping: ExportMapping[];
    returnTableMapping: ReturnTableMapping[];
}