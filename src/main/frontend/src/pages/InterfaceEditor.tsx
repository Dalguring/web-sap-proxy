import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../api/client';
import type { InterfaceDefinition } from '../types/interface';

// --- 상수 및 데이터 정의 ---
const SAP_TYPES = [
    'CHAR', 'NCHAR', 'VARCHAR', 'NVARCHAR', 'STRING', 'SHORTTEXT',
    'TINYINT', 'SMALLINT', 'INT', 'INTEGER', 'BIGINT',
    'DECIMAL', 'DEC', 'SMALLDECIMAL', 'REAL', 'DOUBLE',
    'DATE', 'TIME', 'SECONDDATE', 'TIMESTAMP',
    'BINARY', 'VARBINARY', 'CLOB', 'NCLOB', 'BLOB', 'TEXT',
    'BOOLEAN', 'ARRAY', 'TABLE', 'ST_GEOMETRY', 'ST_POINT'
];

const DEFAULT_TYPE = 'CHAR';
const DETAIL_CACHE_PREFIX = 'cached_interface_detail_';
const SWAGGER_URL = 'http://localhost:8080/swagger-ui/index.html';

const MANDATORY_EXPORTS = [
    { webField: 'E_TYPE', sapParam: 'E_TYPE', type: 'CHAR', size: 1, remarks: '실행 결과 상태 (S/E/P)', example: 'S' },
    { webField: 'E_MESSAGE', sapParam: 'E_MESSAGE', type: 'CHAR', size: 255, remarks: '결과 메시지', example: '' }
];

const initialInterface: InterfaceDefinition = {
    id: '',
    name: '',
    description: '',
    sapModule: '',
    rfcFunction: '',
    executable: false,
    importMapping: [],
    tableMapping: [],
    exportMapping: [],
    returnTableMapping: []
};

type MappingType = 'import' | 'table' | 'export' | 'return';

interface TypeModalTarget {
    mappingType: MappingType;
    rowIndex: number;
    fieldIndex?: number;
}

const InterfaceEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [isEditing, setIsEditing] = useState(false);
    const [activeTab, setActiveTab] = useState<MappingType>('import');
    const [def, setDef] = useState<InterfaceDefinition>(initialInterface);
    const [loading, setLoading] = useState(false);

    const [isTypeModalOpen, setIsTypeModalOpen] = useState(false);
    const [typeSearchTerm, setTypeSearchTerm] = useState('');
    const [typeTarget, setTypeTarget] = useState<TypeModalTarget | null>(null);

    useEffect(() => {
        if (id) {
            setIsEditing(false);
            fetchInterfaceDetail(id);
        } else {
            setIsEditing(true);
            setDef({
                ...initialInterface,
                exportMapping: [...MANDATORY_EXPORTS]
            });
        }
    }, [id]);

    const sanitizeData = (data: InterfaceDefinition): InterfaceDefinition => {
        const fixItem = (item: any) => ({
            ...item,
            type: item.type || DEFAULT_TYPE,
            remarks: item.remarks || '',
            required: item.required === true || item.required === 'true' || item.required === 'Y'
        });

        return {
            ...data,
            executable: data.executable ?? false,
            importMapping: data.importMapping?.map(fixItem) || [],
            exportMapping: data.exportMapping?.map(fixItem) || [],
            tableMapping: data.tableMapping?.map((table: any) => ({
                ...table,
                required: table.required === true || table.required === 'true' || table.required === 'Y',
                singleValue: table.singleValue === true || table.singleValue === 'true' || table.singleValue === 'Y',
                fields: table.fields?.map(fixItem) || []
            })) || [],
            returnTableMapping: data.returnTableMapping?.map((table: any) => ({
                ...table,
                fields: table.fields?.map(fixItem) || []
            })) || []
        };
    };

    // 상세 조회
    const fetchInterfaceDetail = async (interfaceId: string) => {
        const cacheKey = DETAIL_CACHE_PREFIX + interfaceId;
        const cached = sessionStorage.getItem(cacheKey);

        if (cached) {
            setDef(sanitizeData(JSON.parse(cached)));
            return;
        }

        try {
            setLoading(true);
            const response = await client.get(`/proxy/interfaces/${interfaceId}`);
            const result = response.data;

            if (result.success && result.data) {
                const detailData = result.data[interfaceId] || Object.values(result.data)[0];
                if (detailData) {
                    const sanitized = sanitizeData(detailData);
                    setDef(sanitized);
                    sessionStorage.setItem(cacheKey, JSON.stringify(sanitized));
                } else {
                    alert('상세 데이터가 없습니다.');
                }
            } else {
                alert('데이터를 찾을 수 없습니다.');
                navigate('/');
            }
        } catch (error) {
            console.error(error);
            alert('데이터 로딩 실패');
        } finally {
            setLoading(false);
        }
    };

    // 헬퍼
    const getKeys = (type: MappingType) => {
        if (type === 'return') return { web: 'webReturnList', sap: 'sapReturnTable' };
        return { web: 'webFields', sap: 'sapTable' };
    };

    const getTableList = (type: MappingType) => {
        return type === 'return' ? def.returnTableMapping : def.tableMapping;
    };

    // --- 핸들러 ---
    const handleBasicChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
        setDef({ ...def, [e.target.name]: value });
    };

    const handleNumberInput = (e: React.ChangeEvent<HTMLInputElement>, callback: (val: string) => void) => {
        const value = e.target.value.replace(/[^0-9]/g, '');
        callback(value);
    };

    const handleCancel = () => {
        if (id) {
            const cacheKey = DETAIL_CACHE_PREFIX + id;
            const cached = sessionStorage.getItem(cacheKey);
            if (cached) {
                setDef(sanitizeData(JSON.parse(cached)));
            } else {
                fetchInterfaceDetail(id);
            }
            setIsEditing(false);
        } else {
            navigate('/');
        }
    };

    // 행 추가/삭제 로직
    const addRow = (type: MappingType) => {
        const newDef = { ...def };
        const commonFields = { type: DEFAULT_TYPE, size: 0, remarks: '', example: '' };

        if (type === 'import') {
            newDef.importMapping = [...(newDef.importMapping || []), { webField: '', sapField: '', required: true, defaultValue: '', ...commonFields }];
        } else if (type === 'export') {
            newDef.exportMapping = [...(newDef.exportMapping || []), { webField: '', sapParam: '', ...commonFields }];
        } else {
            const targetList = type === 'table' ? 'tableMapping' : 'returnTableMapping';
            const { web, sap } = getKeys(type);
            // @ts-ignore
            newDef[targetList] = [...(newDef[targetList] || []), { [web]: '', [sap]: '', singleValue: false, required: false, fields: [] }];
        }
        setDef(newDef);
    };

    const removeRow = (type: MappingType, index: number) => {
        const newDef = { ...def };

        if (type === 'export') {
            const target = newDef.exportMapping[index];
            if (target.sapParam === 'E_TYPE' || target.sapParam === 'E_MESSAGE') {
                const isConfirmed = window.confirm(
                    `'${target.sapParam}'은 표준 권장 파라미터입니다.\n삭제 시 Web에서 결과 상태를 판단하지 못할 수 있습니다.\n\n정말 삭제하시겠습니까?`
                );
                if (!isConfirmed) return;
            }
        }

        if (type === 'import') newDef.importMapping.splice(index, 1);
        else if (type === 'export') newDef.exportMapping.splice(index, 1);
        else if (type === 'table') newDef.tableMapping.splice(index, 1);
        else newDef.returnTableMapping.splice(index, 1);
        setDef(newDef);
    };

    const handleMappingChange = (type: 'import' | 'export', index: number, field: string, value: any) => {
        const newDef = { ...def };
        const list = type === 'import' ? newDef.importMapping : newDef.exportMapping;
        // @ts-ignore
        list[index][field] = value;
        setDef(newDef);
    };

    const addTableField = (type: MappingType, tableIndex: number) => {
        const newDef = { ...def };
        const list = type === 'return' ? newDef.returnTableMapping : newDef.tableMapping;
        if (!list[tableIndex].fields) list[tableIndex].fields = [];
        list[tableIndex].fields.push({ webField: '', sapField: '', type: DEFAULT_TYPE, required: false, size: 0, defaultValue: '', example: '', remarks: '' });
        setDef(newDef);
    };

    const removeTableField = (type: MappingType, tableIndex: number, fieldIndex: number) => {
        const newDef = { ...def };
        const list = type === 'return' ? newDef.returnTableMapping : newDef.tableMapping;
        list[tableIndex].fields.splice(fieldIndex, 1);
        setDef(newDef);
    };

    const handleTableFieldChange = (type: MappingType, tableIndex: number, fieldIndex: number, key: string, value: any) => {
        const newDef = { ...def };
        const list = type === 'return' ? newDef.returnTableMapping : newDef.tableMapping;
        // @ts-ignore
        list[tableIndex].fields[fieldIndex][key] = value;
        setDef(newDef);
    };

    const handleTablePropChange = (type: MappingType, tableIndex: number, key: string, value: any) => {
        const newDef = { ...def };
        const list = type === 'return' ? newDef.returnTableMapping : newDef.tableMapping;
        // @ts-ignore
        list[tableIndex][key] = value;
        setDef(newDef);
    };

    const openTypeModal = (mappingType: MappingType, rowIndex: number, fieldIndex?: number) => {
        setTypeTarget({ mappingType, rowIndex, fieldIndex });
        setTypeSearchTerm('');
        setIsTypeModalOpen(true);
    };

    const handleTypeSelect = (selectedType: string) => {
        if (!typeTarget) return;
        const { mappingType, rowIndex, fieldIndex } = typeTarget;

        if (mappingType === 'import' || mappingType === 'export') {
            handleMappingChange(mappingType, rowIndex, 'type', selectedType);
        } else {
            if (fieldIndex !== undefined) {
                handleTableFieldChange(mappingType, rowIndex, fieldIndex, 'type', selectedType);
            }
        }
        setIsTypeModalOpen(false);
    };

    const filteredTypes = useMemo(() => {
        if (!typeSearchTerm) return SAP_TYPES;
        return SAP_TYPES.filter(t => t.toLowerCase().includes(typeSearchTerm.toLowerCase()));
    }, [typeSearchTerm]);

    // --- 저장/삭제 로직 ---
    const validate = async (): Promise<boolean> => {
        if (!def.id || !def.rfcFunction) {
            alert('ID와 RFC Function은 필수입니다.');
            return false;
        }
        if (!id) {
            try {
                const checkRes = await client.get(`/proxy/interfaces/${def.id.toUpperCase()}`);
                if (checkRes.data.success) {
                    alert(`이미 존재하는 ID입니다: ${def.id.toUpperCase()}`);
                    return false;
                }
            } catch (e: any) {
                if (e.response && e.response.status !== 404) console.error("중복 체크 오류", e);
            }
        }
        return true;
    };

    const handleSave = async () => {
        if (!(await validate())) return;
        try {
            const sanitizedDef = sanitizeData(def);
            const payload = {
                ...sanitizedDef,
                id: sanitizedDef.id.toUpperCase(),
                rfcFunction: sanitizedDef.rfcFunction.toUpperCase()
            };
            await client.post('/admin/interfaces/save', payload);
            sessionStorage.removeItem('cached_interfaces');
            if (payload.id) sessionStorage.removeItem(DETAIL_CACHE_PREFIX + payload.id);
            alert('저장되었습니다.');
            if (!id) navigate(`/edit/${payload.id}`);
            else {
                setDef(payload);
                setIsEditing(false);
            }
        } catch (e: any) {
            alert(`저장 실패: ${e.response?.data?.message || e.message}`);
        }
    };

    const handleDelete = async () => {
        if (!window.confirm(`정말 '${def.id}'를 삭제하시겠습니까?`)) return;
        try {
            await client.delete(`/admin/interfaces/${def.id}`);
            sessionStorage.removeItem('cached_interfaces');
            sessionStorage.removeItem(DETAIL_CACHE_PREFIX + def.id);
            alert('삭제되었습니다.');
            navigate('/');
        } catch (e: any) {
            alert(`삭제 실패: ${e.message}`);
        }
    };

    const copyToClipboard = async (text: string) => {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(text);
        } else {
            return new Promise<void>((resolve, reject) => {
                try {
                    const textArea = document.createElement("textarea");
                    textArea.value = text;
                    textArea.style.position = "fixed";
                    textArea.style.left = "-9999px";
                    document.body.appendChild(textArea);
                    textArea.focus();
                    textArea.select();
                    document.execCommand('copy');
                    document.body.removeChild(textArea);
                    resolve();
                } catch (err) {
                    reject(err);
                }
            });
        }
    };

    // --- Swagger 실행을 위한 JSON 생성 및 이동 로직 ---
    const handleExecuteSwagger = () => {
        const payload: Record<string, any> = {};

        def.importMapping.forEach(item => {
            if (item.webField) {
                const isRequired = item.required === true || (item.required as any) === 'true';
                const hasValue = item.defaultValue || item.example;

                if (isRequired || hasValue) {
                    payload[item.webField] = item.defaultValue || item.example || "";
                }
            }
        });

        def.tableMapping.forEach(table => {
            // @ts-ignore
            const listKey = table.webFields || table.webListKey;

            if (listKey) {
                const isTableRequired = table.required || (table.required as any) === 'true';
                const hasFieldValues = table.fields?.some(f => f.defaultValue || f.example);

                if (!isTableRequired && !hasFieldValues) {
                    payload[listKey] = [];
                } else {
                    const rowData: Record<string, any> = {};

                    table.fields?.forEach(field => {
                        if (field.webField) {
                            const isFieldRequired = field.required === true || (field.required as any) === 'true';
                            const hasFieldValue = field.defaultValue || field.example;

                            if (isFieldRequired || hasFieldValue) {
                                rowData[field.webField] = field.defaultValue || field.example || "";
                            }
                        }
                    });
                    payload[listKey] = [rowData];
                }
            }
        });

        const fullPayload = {
            interfaceId: def.id,
            userId: "TEST_USER",
            data: payload
        };

        const jsonString = JSON.stringify(fullPayload, null, 2);

        copyToClipboard(jsonString).then(() => {
            alert("📋 JSON 데이터가 클립보드에 복사되었습니다!\nSwagger의 'Try it out' -> Request Body에 붙여넣기 하고 테스트 할 값을 입력하세요.");
            window.open(SWAGGER_URL, '_blank');
        }).catch(err => {
            console.error('클립보드 복사 실패:', err);
            alert('클립보드 복사에 실패했습니다. Swagger를 바로 엽니다.');
            window.open(SWAGGER_URL, '_blank');
        });
    };

    if (loading) return <div>Loading...</div>;

    // --- 뷰 모드 렌더링 (읽기 전용) ---
    const renderReadOnlyTable = (type: MappingType) => {
        const list = type === 'import' ? def.importMapping
                : type === 'export' ? def.exportMapping
                        : getTableList(type);

        const { web, sap } = (type === 'table' || type === 'return') ? getKeys(type) : { web: '', sap: '' };
        const isTableType = (type === 'table' || type === 'return');
        const hasDefaultValues = type === 'import' || type === 'table';

        if (!list || list.length === 0) return <div className="text-gray-400 text-sm py-2">데이터가 없습니다.</div>;

        return (
                <div className="overflow-x-auto border rounded mb-6 shadow-sm">
                    <table className="min-w-full divide-y divide-gray-200 text-sm table-fixed">
                        <colgroup>
                            {isTableType ? (
                                    <>
                                        <col style={{ width: 'auto' }} />
                                        <col style={{ width: 'auto' }} />
                                        <col style={{ width: 'auto' }} />
                                        <col style={{ width: 'auto' }} />
                                    </>
                            ) : (
                                    <>
                                        <col style={{ width: 'auto' }} />
                                        <col style={{ width: 'auto' }} />
                                    </>
                            )}
                            <col style={{ width: '80px' }} />
                            <col style={{ width: '100px' }} />
                            <col style={{ width: '60px' }} />
                            <col style={{ width: '200px' }} />
                            <col style={{ width: '150px' }} />
                        </colgroup>
                        <thead className="bg-gray-100 font-bold text-gray-700">
                        <tr>
                            {isTableType && (
                                    <>
                                        <th className="px-4 py-2 border-r text-left">Web List Key</th>
                                        <th className="px-4 py-2 border-r text-left">SAP Table</th>
                                    </>
                            )}
                            <th className="px-4 py-2 border-r text-left">Web Field</th>
                            <th className="px-4 py-2 border-r text-left">SAP Field</th>
                            <th className={`px-4 py-2 border-r text-center ${hasDefaultValues ? 'bg-yellow-50 text-yellow-800' : ''}`}>
                                {hasDefaultValues ? 'Default' : ''}
                            </th>
                            <th className="px-4 py-2 border-r text-center">Type</th>
                            <th className="px-4 py-2 border-r text-center">Len</th>
                            <th className="px-4 py-2 border-r text-left text-gray-600">Remark</th>
                            <th className="px-4 py-2 text-left bg-blue-50 text-blue-800">Example</th>
                        </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 bg-white">
                        {list.map((item: any, idx: number) => {
                            if (isTableType) {
                                if (item.fields && item.fields.length > 0) {
                                    return item.fields.map((field: any, fIdx: number) => (
                                            <tr key={`${idx}-${fIdx}`} className="hover:bg-gray-50">
                                                {fIdx === 0 && (
                                                        <>
                                                            <td rowSpan={item.fields.length} className="px-4 py-2 border-r align-top bg-gray-50 font-medium truncate">
                                                                {item.required && <span className="text-red-500 font-bold mr-1" title="Required">*</span>}
                                                                {item[web]}
                                                            </td>
                                                            <td rowSpan={item.fields.length} className="px-4 py-2 border-r align-top bg-gray-50 font-medium text-blue-600 truncate" title={item[sap]}>{item[sap]}</td>
                                                        </>
                                                )}
                                                <td className="px-4 py-2 border-r truncate">
                                                    {hasDefaultValues && field.required && <span className="text-red-500 mr-1">*</span>}
                                                    {field.webField}
                                                </td>
                                                <td className="px-4 py-2 border-r truncate" title={field.sapField}>{field.sapField}</td>
                                                <td className="px-4 py-2 border-r bg-yellow-50/30 text-gray-600 truncate text-center">
                                                    {hasDefaultValues ? field.defaultValue : ''}
                                                </td>
                                                <td className="px-4 py-2 border-r text-center truncate">{field.type || DEFAULT_TYPE}</td>
                                                <td className="px-4 py-2 border-r text-center">{field.size}</td>
                                                <td className="px-4 py-2 border-r text-gray-500 truncate" title={field.remarks}>{field.remarks || '-'}</td>
                                                <td className="px-4 py-2 bg-blue-50/30 text-gray-600 truncate">{field.example || '-'}</td>
                                            </tr>
                                    ));
                                } else {
                                    return (
                                            <tr key={idx} className="bg-red-50">
                                                <td className="px-4 py-2 border-r">
                                                    {hasDefaultValues && item.required && <span className="text-red-500 mr-1">*</span>}
                                                    {item[web]}
                                                </td>
                                                <td className="px-4 py-2 border-r text-blue-600">{item[sap]}</td>
                                                <td colSpan={7} className="px-4 py-2 text-center text-red-400 italic">필드 없음</td>
                                            </tr>
                                    );
                                }
                            } else {
                                return (
                                        <tr key={idx} className="hover:bg-gray-50">
                                            <td className="px-4 py-2 border-r truncate" title={item.webField}>
                                                {hasDefaultValues && item.required && <span className="text-red-500 mr-1">*</span>}
                                                {item.webField}
                                            </td>
                                            <td className="px-4 py-2 border-r truncate" title={type === 'export' ? item.sapParam : item.sapField}>{type === 'export' ? item.sapParam : item.sapField}</td>
                                            <td className="px-4 py-2 border-r bg-yellow-50/30 text-gray-600 truncate text-center">
                                                {hasDefaultValues ? item.defaultValue : ''}
                                            </td>
                                            <td className="px-4 py-2 border-r text-center truncate">{item.type || DEFAULT_TYPE}</td>
                                            <td className="px-4 py-2 border-r text-center">{item.size}</td>
                                            <td className="px-4 py-2 border-r text-gray-500 truncate" title={item.remarks}>{item.remarks || '-'}</td>
                                            <td className="px-4 py-2 bg-blue-50/30 text-gray-600 truncate">{item.example || '-'}</td>
                                        </tr>
                                );
                            }
                        })}
                        </tbody>
                    </table>
                </div>
        );
    }

    // --- 메인 렌더링 (View Mode) ---
    if (!isEditing) {
        return (
                <div className="bg-white shadow-lg rounded-lg min-h-[80vh] p-8">
                    <div className="flex justify-between items-start mb-8 border-b pb-4">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
                                <span className="bg-blue-600 text-white text-xs px-2 py-1 rounded">IF ID</span>
                                {def.id}
                            </h1>
                            <h2 className="text-xl text-gray-700 mt-2 font-semibold">{def.name || '(No Name)'}</h2>
                            <p className="text-gray-500 mt-1 text-sm">{def.description}</p>
                            <div className="flex gap-2 mt-2">
                                <div className="text-sm font-mono bg-gray-100 inline-block px-2 py-1 rounded text-gray-700">
                                    RFC: <strong>{def.rfcFunction}</strong>
                                </div>
                                {def.sapModule && (
                                        <div className="text-sm font-mono bg-yellow-100 inline-block px-2 py-1 rounded text-yellow-800">
                                            Module: <strong>{def.sapModule}</strong>
                                        </div>
                                )}
                                {def.executable && (
                                        <div className="text-sm font-bold bg-green-100 text-green-700 inline-flex items-center px-2 py-1 rounded border border-green-200">
                                            ✅ Executable
                                        </div>
                                )}
                            </div>
                        </div>
                        <div className="flex gap-2">
                            {def.executable && (
                                    <button
                                            onClick={handleExecuteSwagger}
                                            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 font-bold shadow flex items-center gap-1"
                                    >
                                        ▶ 실행하기
                                    </button>
                            )}
                            <button onClick={() => navigate('/')} className="px-4 py-2 border rounded hover:bg-gray-50">목록으로</button>
                            <button onClick={() => setIsEditing(true)} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 font-bold">수정하기</button>
                        </div>
                    </div>
                    <div className="space-y-8">
                        <section><h2 className="text-lg font-bold text-gray-700 mb-2 border-l-4 border-green-500 pl-2">Import Parameters</h2>{renderReadOnlyTable('import')}</section>
                        <section><h2 className="text-lg font-bold text-gray-700 mb-2 border-l-4 border-blue-500 pl-2">Table Parameters</h2>{renderReadOnlyTable('table')}</section>
                        <section><h2 className="text-lg font-bold text-gray-700 mb-2 border-l-4 border-orange-500 pl-2">Export Parameters</h2>{renderReadOnlyTable('export')}</section>
                        <section><h2 className="text-lg font-bold text-gray-700 mb-2 border-l-4 border-purple-500 pl-2">Return Table</h2>{renderReadOnlyTable('return')}</section>
                    </div>
                </div>
        );
    }

    // --- 메인 렌더링 (Edit Mode) ---
    const { web, sap } = (activeTab === 'table' || activeTab === 'return') ? getKeys(activeTab) : { web: '', sap: '' };
    const isReqEnabled = activeTab === 'import' || activeTab === 'table';

    const gridStyle = {
        display: 'grid',
        gridTemplateColumns: isReqEnabled
                ? '40px 1.2fr 1.2fr 100px 100px 60px 1.5fr 1.5fr 40px'
                : '1.2fr 1.2fr 100px 60px 1.5fr 1.5fr 40px',
        gap: '0.5rem',
        alignItems: 'center'
    };

    return (
            <div className="bg-white shadow-lg rounded-lg min-h-[80vh] flex flex-col border-2 border-blue-500 relative">

                {/* 데이터 타입 검색 모달 */}
                {isTypeModalOpen && (
                        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
                            <div className="bg-white rounded-lg shadow-xl w-96 max-h-[80vh] flex flex-col">
                                <div className="p-4 border-b flex justify-between items-center bg-gray-50 rounded-t-lg">
                                    <h3 className="font-bold text-lg text-gray-700">데이터 타입 선택</h3>
                                    <button onClick={() => setIsTypeModalOpen(false)} className="text-gray-500 hover:text-black font-bold">✕</button>
                                </div>
                                <div className="p-2 border-b">
                                    <input
                                            type="text"
                                            className="w-full border rounded p-2 focus:ring-2 focus:ring-blue-500 outline-none"
                                            placeholder="타입 검색 (ex: CHAR, INT...)"
                                            autoFocus
                                            value={typeSearchTerm}
                                            onChange={(e) => setTypeSearchTerm(e.target.value)}
                                    />
                                </div>
                                <div className="overflow-y-auto flex-1 p-2">
                                    {filteredTypes.map(t => (
                                            <button
                                                    key={t}
                                                    onClick={() => handleTypeSelect(t)}
                                                    className="w-full text-left px-4 py-2 hover:bg-blue-50 hover:text-blue-600 rounded transition-colors text-sm font-mono"
                                            >
                                                {t}
                                            </button>
                                    ))}
                                    {filteredTypes.length === 0 && <div className="text-center text-gray-400 py-4">검색 결과가 없습니다.</div>}
                                </div>
                            </div>
                        </div>
                )}

                <div className="p-4 bg-blue-50 border-b border-blue-200 flex justify-between items-center">
                    <span className="font-bold text-blue-800 text-lg">✏️ 편집 모드</span>
                    <div className="flex items-center gap-4">
                        <label className="flex items-center gap-2 cursor-pointer bg-white px-3 py-1 rounded border border-blue-200 hover:bg-blue-50">
                            <input
                                    type="checkbox"
                                    name="executable"
                                    checked={def.executable || false}
                                    onChange={handleBasicChange}
                                    className="w-4 h-4 text-blue-600 accent-blue-600"
                            />
                            <span className="text-sm font-bold text-gray-700">실행 가능 여부</span>
                        </label>

                        <div className="space-x-2 border-l pl-4 border-blue-200">
                            <button onClick={handleCancel} className="px-3 py-1 bg-white border border-gray-300 rounded text-sm">취소</button>
                            <button onClick={handleSave} className="px-4 py-1 bg-blue-600 text-white rounded text-sm font-bold hover:bg-blue-700 shadow">저장 완료</button>
                        </div>
                    </div>
                </div>

                <div className="p-6 border-b bg-white">
                    <div className="grid grid-cols-2 gap-6 mb-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Interface ID (URL)</label>
                            <input type="text" name="id" value={def.id} onChange={handleBasicChange} disabled={!!id} className="mt-1 block w-full border rounded p-2 bg-gray-50" placeholder="예: WMS_GOODS_MOVE" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">SAP Module</label>
                            <input type="text" name="sapModule" value={def.sapModule || ''} onChange={handleBasicChange} className="mt-1 block w-full border rounded p-2" placeholder="예: MM, SD" />
                        </div>
                    </div>
                    <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700">RFC Function Name</label>
                        <input type="text" name="rfcFunction" value={def.rfcFunction} onChange={handleBasicChange} className="mt-1 block w-full border rounded p-2" placeholder="예: ZPP_IF_WMS_..." />
                    </div>
                    <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700">Interface Name (한글 명칭)</label>
                        <input type="text" name="name" value={def.name || ''} onChange={handleBasicChange} className="mt-1 block w-full border rounded p-2" placeholder="예: 재고 이동 인터페이스" />
                    </div>
                    <div><label className="block text-sm font-medium text-gray-700">Description</label><input type="text" name="description" value={def.description} onChange={handleBasicChange} className="mt-1 block w-full border rounded p-2" /></div>
                </div>

                <div className="flex border-b border-gray-200 bg-gray-50">
                    {['import', 'table', 'export', 'return'].map((tab) => (
                            <button key={tab} onClick={() => setActiveTab(tab as MappingType)} className={`py-3 px-6 text-center font-medium text-sm border-r ${activeTab === tab ? 'bg-white text-blue-600 border-t-2 border-t-blue-500' : 'text-gray-500 hover:bg-gray-100'}`}>{tab.toUpperCase()}</button>
                    ))}
                </div>

                <div className="flex-grow p-6 bg-gray-100 overflow-y-auto">
                    <div className="bg-white rounded border shadow-sm p-4">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-bold text-gray-700">{activeTab.toUpperCase()} Mapping</h3>
                            <button onClick={() => addRow(activeTab)} className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-700">+ {activeTab === 'table' || activeTab === 'return' ? '테이블 추가' : '행 추가'}</button>
                        </div>

                        {(activeTab === 'import' || activeTab === 'export') && (
                                <div style={gridStyle} className="text-xs font-bold text-gray-500 uppercase px-2 mb-2">
                                    {isReqEnabled && <div className="text-center text-red-600">REQ</div>}

                                    <div>Web Field</div>
                                    <div>SAP Field</div>
                                    <div>Type</div>

                                    {isReqEnabled ? (
                                            <>
                                                <div className="text-yellow-700">Default</div>
                                                <div className="text-center">Len</div>
                                                <div>Remark</div>
                                                <div className="text-blue-600">Ex</div>
                                            </>
                                    ) : (
                                            <>
                                                <div className="text-center">Len</div>
                                                <div>Remark</div>
                                                <div className="text-blue-600">Example</div>
                                            </>
                                    )}
                                    <div className="text-center">Del</div>
                                </div>
                        )}

                        {(activeTab === 'import' || activeTab === 'export') && (
                                <div className="space-y-2">
                                    {(activeTab === 'import' ? def.importMapping : def.exportMapping)?.map((row: any, idx: number) => {
                                            const isStandard = activeTab === 'export' && (row.sapParam === 'E_TYPE' || row.sapParam === 'E_MESSAGE');
                                            return (
                                                <div key={idx} style={gridStyle} className="bg-gray-50 p-2 rounded border hover:bg-gray-100 transition-colors">
                                                    {isReqEnabled && (
                                                            <div className="text-center">
                                                                <input type="checkbox" checked={row.required || false} onChange={(e) => handleMappingChange(activeTab, idx, 'required', e.target.checked)} className="w-5 h-5 text-blue-600 accent-blue-600"/>
                                                            </div>
                                                    )}

                                                    <div><input type="text" value={row.webField} onChange={(e) => handleMappingChange(activeTab, idx, 'webField', e.target.value)} className="w-full border rounded p-1 text-sm" placeholder="Web"/></div>
                                                    <div><input type="text" value={activeTab === 'export' ? row.sapParam : row.sapField} onChange={(e) => handleMappingChange(activeTab, idx, activeTab === 'export' ? 'sapParam' : 'sapField', e.target.value)} className={`w-full border rounded p-1 text-sm ${isStandard ? 'border-blue-300 bg-blue-50' : 'bg-yellow-50'}`} placeholder="SAP"/></div>

                                                    <div>
                                                        <div
                                                                onClick={() => openTypeModal(activeTab, idx)}
                                                                className="w-full border rounded p-1 text-sm text-center bg-white cursor-pointer hover:border-blue-500 flex justify-between items-center px-2"
                                                        >
                                                            <span className="truncate">{row.type || 'CHAR'}</span>
                                                            <span className="text-[10px] text-gray-400">▼</span>
                                                        </div>
                                                    </div>

                                                    {isReqEnabled ? (
                                                            <>
                                                                <div><input type="text" value={row.defaultValue || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'defaultValue', e.target.value)} className="w-full border rounded p-1 text-sm bg-yellow-50" placeholder="기본값"/></div>
                                                                <div><input type="text" value={row.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleMappingChange(activeTab, idx, 'size', val))} className="w-full border rounded p-1 text-sm text-center" placeholder="Len"/></div>
                                                                <div><input type="text" value={row.remarks || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'remarks', e.target.value)} className="w-full border rounded p-1 text-sm text-gray-600" placeholder="Remark"/></div>
                                                                <div><input type="text" value={row.example || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'example', e.target.value)} className="w-full border rounded p-1 text-sm bg-blue-50" placeholder="Ex"/></div>
                                                            </>
                                                    ) : (
                                                            <>
                                                                <div><input type="text" value={row.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleMappingChange(activeTab, idx, 'size', val))} className="w-full border rounded p-1 text-sm text-center" placeholder="Len"/></div>
                                                                <div><input type="text" value={row.remarks || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'remarks', e.target.value)} className="w-full border rounded p-1 text-sm text-gray-600" placeholder="Remark"/></div>
                                                                <div><input type="text" value={row.example || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'example', e.target.value)} className="w-full border rounded p-1 text-sm bg-blue-50" placeholder="예시"/></div>
                                                            </>
                                                    )}

                                                    <div className="text-center"><button onClick={() => removeRow(activeTab, idx)} className="text-red-500 font-bold hover:bg-red-100 rounded w-6 h-6 flex items-center justify-center mx-auto">×</button></div>
                                                </div>
                                            );
                                    })}
                                </div>
                        )}

                        {(activeTab === 'table' || activeTab === 'return') && (
                                <div className="space-y-6">
                                    {getTableList(activeTab)?.map((table: any, tIdx: number) => (
                                            <div key={tIdx} className="border-2 border-blue-100 rounded-lg bg-white overflow-hidden shadow-sm">
                                                <div className="bg-blue-50 p-3 border-b flex items-center gap-4">

                                                    {activeTab === 'table' && (
                                                            <label className="flex items-center gap-1 text-sm text-red-600 font-bold bg-white px-3 py-1 rounded border border-red-200 shadow-sm cursor-pointer hover:bg-red-50">
                                                                <input type="checkbox" checked={table.required || false} onChange={(e) => handleTablePropChange(activeTab, tIdx, 'required', e.target.checked)} className="text-red-600 accent-red-600"/>
                                                                <span>REQ</span>
                                                            </label>
                                                    )}

                                                    <div className="flex-1"><input type="text" value={table[web]} onChange={(e) => handleTablePropChange(activeTab, tIdx, web, e.target.value)} className="w-full border rounded px-2 py-1 text-sm font-bold text-blue-800" placeholder="Web List Key"/></div>
                                                    <span className="text-gray-400">↔</span>
                                                    <div className="flex-1"><input type="text" value={table[sap]} onChange={(e) => handleTablePropChange(activeTab, tIdx, sap, e.target.value)} className="w-full border rounded px-2 py-1 text-sm font-bold text-blue-800 bg-yellow-50" placeholder="SAP Table"/></div>

                                                    <button onClick={() => removeRow(activeTab, tIdx)} className="bg-red-100 text-red-600 px-3 py-1 rounded text-xs font-bold ml-2">테이블 삭제</button>
                                                </div>
                                                <div className="p-3 bg-gray-50">
                                                    <div className="flex justify-between mb-2"><span className="text-xs font-semibold text-gray-500">Fields Mapping</span><button onClick={() => addTableField(activeTab, tIdx)} className="text-blue-600 text-xs border bg-white px-2 py-1 rounded">+ 필드 추가</button></div>

                                                    <div style={gridStyle} className="text-[10px] text-gray-500 uppercase px-1 mb-1">
                                                        {isReqEnabled && <div className="text-center text-red-600">REQ</div>}

                                                        <div>Web</div>
                                                        <div>SAP</div>
                                                        <div>Type</div>

                                                        {isReqEnabled ? (
                                                                <>
                                                                    <div className="text-yellow-700">Default</div>
                                                                    <div className="text-center">Len</div>
                                                                    <div>Remark</div>
                                                                    <div className="text-blue-600">Ex</div>
                                                                </>
                                                        ) : (
                                                                <>
                                                                    <div className="text-center">Len</div>
                                                                    <div>Remark</div>
                                                                    <div className="text-blue-600">Ex</div>
                                                                </>
                                                        )}
                                                        <div></div>
                                                    </div>

                                                    <div className="space-y-1">
                                                        {table.fields?.map((field: any, fIdx: number) => (
                                                                <div key={fIdx} style={gridStyle} className="items-center">
                                                                    {isReqEnabled && (
                                                                            <div className="text-center">
                                                                                <input type="checkbox" checked={field.required || false} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'required', e.target.checked)} className="w-4 h-4 accent-blue-600"/>
                                                                            </div>
                                                                    )}

                                                                    <div><input type="text" value={field.webField} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'webField', e.target.value)} className="w-full border rounded p-1 text-sm"/></div>
                                                                    <div><input type="text" value={field.sapField} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'sapField', e.target.value)} className="w-full border rounded p-1 text-sm bg-yellow-50"/></div>

                                                                    <div>
                                                                        <div
                                                                                onClick={() => openTypeModal(activeTab, tIdx, fIdx)}
                                                                                className="w-full border rounded p-1 text-sm text-center bg-white cursor-pointer hover:border-blue-500 flex justify-between items-center px-2"
                                                                        >
                                                                            <span className="truncate">{field.type || 'CHAR'}</span>
                                                                            <span className="text-[10px] text-gray-400">▼</span>
                                                                        </div>
                                                                    </div>

                                                                    {isReqEnabled ? (
                                                                            <>
                                                                                <div><input type="text" value={field.defaultValue || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'defaultValue', e.target.value)} className="w-full border rounded p-1 text-xs bg-yellow-50"/></div>
                                                                                <div><input type="text" value={field.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleTableFieldChange(activeTab, tIdx, fIdx, 'size', val))} className="w-full border rounded p-1 text-xs text-center"/></div>
                                                                                <div><input type="text" value={field.remarks || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'remarks', e.target.value)} className="w-full border rounded p-1 text-xs text-gray-600"/></div>
                                                                                <div><input type="text" value={field.example || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'example', e.target.value)} className="w-full border rounded p-1 text-xs bg-blue-50"/></div>
                                                                            </>
                                                                    ) : (
                                                                            <>
                                                                                <div><input type="text" value={field.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleTableFieldChange(activeTab, tIdx, fIdx, 'size', val))} className="w-full border rounded p-1 text-xs text-center"/></div>
                                                                                <div><input type="text" value={field.remarks || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'remarks', e.target.value)} className="w-full border rounded p-1 text-xs text-gray-600"/></div>
                                                                                <div><input type="text" value={field.example || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'example', e.target.value)} className="w-full border rounded p-1 text-xs bg-blue-50"/></div>
                                                                            </>
                                                                    )}

                                                                    <div className="text-center"><button onClick={() => removeTableField(activeTab, tIdx, fIdx)} className="text-gray-400 hover:text-red-500 font-bold">×</button></div>
                                                                </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            </div>
                                    ))}
                                </div>
                        )}
                    </div>
                </div>

                <div className="p-4 border-t bg-gray-100 flex justify-between">
                    <div>
                        {id && (
                                <button onClick={handleDelete} className="px-4 py-2 bg-red-100 text-red-600 border border-red-200 rounded shadow-sm hover:bg-red-200 font-bold">삭제</button>
                        )}
                    </div>
                    <div className="flex gap-3">
                        <button onClick={handleCancel} className="px-4 py-2 bg-white border border-gray-300 rounded shadow-sm text-gray-700 hover:bg-gray-50">취소</button>
                        <button onClick={handleSave} className="px-6 py-2 bg-blue-600 text-white rounded shadow-sm hover:bg-blue-700 font-bold">저장하기</button>
                    </div>
                </div>
            </div>
    );
}

export default InterfaceEditor;