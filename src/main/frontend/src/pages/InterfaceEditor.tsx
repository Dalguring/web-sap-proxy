import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../api/client';
import type { InterfaceDefinition } from '../types/interface';

const DETAIL_CACHE_PREFIX = 'cached_interface_detail_';

// SAP Type 목록 정의
const SAP_TYPES = ['STRING', 'CHAR', 'DECIMAL', 'NUMBER', 'DATE', 'DATETIME', 'INT', 'ARRAY', 'STRUCT', 'TABLE'];

// 초기 빈 데이터
const initialInterface: InterfaceDefinition = {
    id: '', name: '', description: '', rfcFunction: '',
    importMapping: [], tableMapping: [], exportMapping: [], returnTableMapping: []
};

type MappingType = 'import' | 'table' | 'export' | 'return';

const InterfaceEditor = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    // [상태 관리]
    const [isEditing, setIsEditing] = useState(false);
    const [activeTab, setActiveTab] = useState<MappingType>('import');
    const [def, setDef] = useState<InterfaceDefinition>(initialInterface);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (id) {
            setIsEditing(false);
            fetchInterfaceDetail(id);
        } else {
            setIsEditing(true);
        }
    }, [id]);

    const fetchInterfaceDetail = async (interfaceId: string) => {
        const cacheKey = DETAIL_CACHE_PREFIX + interfaceId;
        const cached = sessionStorage.getItem(cacheKey);

        if (cached) {
            setDef(JSON.parse(cached));
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            const response = await client.get(`/proxy/interfaces/${interfaceId}`);
            const result = response.data;
            const dataMap = result.data?.interface || result.data;
            if (result.success && result.data) {
                const detailData = result.data[interfaceId] || Object.values(result.data)[0];

                if (detailData) {
                    setDef(detailData);
                    sessionStorage.setItem(cacheKey, JSON.stringify(detailData));
                } else {
                    alert('인터페이스 상세 정보가 비어있습니다.');
                }
            } else {
                alert(result.message || '인터페이스 정보를 찾을 수 없습니다.');
                navigate('/');
            }
        } catch (error) {
            console.error(error);
            alert('데이터를 불러오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // --- 헬퍼 함수 ---
    const getKeys = (type: MappingType) => {
        if (type === 'return') return { web: 'webReturnList', sap: 'sapReturnTable' };
        return { web: 'webFields', sap: 'sapTable' };
    };

    const getTableList = (type: MappingType) => {
        return type === 'return' ? def.returnTableMapping : def.tableMapping;
    };

    // --- 핸들러 ---
    const handleBasicChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setDef({ ...def, [e.target.name]: e.target.value });
    };

    const handleNumberInput = (e: React.ChangeEvent<HTMLInputElement>, callback: (val: string) => void) => {
        const value = e.target.value.replace(/[^0-9]/g, '');
        callback(value);
    };

    const addRow = (type: MappingType) => {
        const newDef = { ...def };
        if (type === 'import') {
            newDef.importMapping = [...(newDef.importMapping || []), { webField: '', sapField: '', type: 'STRING', required: true, size: 0, example: '' }];
        } else if (type === 'export') {
            newDef.exportMapping = [...(newDef.exportMapping || []), { webField: '', sapParam: '', type: 'STRING', size: 0, example: '' }];
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
        list[tableIndex].fields.push({ webField: '', sapField: '', type: 'STRING', required: false, size: 0, example: '' });
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

    // --- 유효성 검사 및 저장 ---
    const validate = (): boolean => {
        if (!def.id || !def.rfcFunction) {
            alert('기본 정보(ID, RFC Function)는 필수입니다.');
            return false;
        }
        return true;
    };

    const handleSave = async () => {
        if (!validate()) return;
        try {
            console.log('Saving...', def);
            // TODO: 실제 API 저장 호출

            sessionStorage.removeItem('cached_interfaces');
            if (def.id) {
                sessionStorage.removeItem(DETAIL_CACHE_PREFIX + def.id);
            }

            alert('저장되었습니다.');
            setIsEditing(false);
        } catch (e) {
            alert('저장 실패');
        }
    };

    // --- 뷰 모드 렌더링 ---
    const renderReadOnlyTable = (type: MappingType) => {
        const list = type === 'import' ? def.importMapping
                : type === 'export' ? def.exportMapping
                        : getTableList(type);

        const { web, sap } = (type === 'table' || type === 'return') ? getKeys(type) : { web: '', sap: '' };
        const isTableType = (type === 'table' || type === 'return');

        if (!list || list.length === 0) return <div className="text-gray-400 text-sm py-2">데이터가 없습니다.</div>;

        return (
                <div className="overflow-x-auto border rounded mb-6 shadow-sm">
                    <table className="min-w-full divide-y divide-gray-200 text-sm table-fixed">
                        <colgroup>
                            {isTableType ? (
                                    <>
                                        <col style={{ width: '17.5%' }} />
                                        <col style={{ width: '17.5%' }} />
                                        <col style={{ width: '17.5%' }} />
                                        <col style={{ width: '17.5%' }} />
                                    </>
                            ) : (
                                    <>
                                        <col style={{ width: '35%' }} />
                                        <col style={{ width: '35%' }} />
                                    </>
                            )}
                            <col style={{ width: '10%' }} />
                            <col style={{ width: '5%' }} />
                            <col style={{ width: '15%' }} />
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
                            <th className="px-4 py-2 border-r text-center">Type</th>
                            <th className="px-4 py-2 border-r text-center">Len</th>
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
                                                            <td rowSpan={item.fields.length} className="px-4 py-2 border-r align-top bg-gray-50 font-medium truncate" title={item[web]}>{item[web]}</td>
                                                            <td rowSpan={item.fields.length} className="px-4 py-2 border-r align-top bg-gray-50 font-medium text-blue-600 truncate" title={item[sap]}>{item[sap]}</td>
                                                        </>
                                                )}
                                                <td className="px-4 py-2 border-r truncate" title={field.webField}>{field.webField}</td>
                                                <td className="px-4 py-2 border-r truncate" title={field.sapField}>{field.sapField}</td>
                                                <td className="px-4 py-2 border-r text-center truncate">{field.type}</td>
                                                <td className="px-4 py-2 border-r text-center">{field.size}</td>
                                                <td className="px-4 py-2 bg-blue-50/30 text-gray-600 truncate">{field.example || '-'}</td>
                                            </tr>
                                    ));
                                } else {
                                    return (
                                            <tr key={idx} className="bg-red-50">
                                                <td className="px-4 py-2 border-r">{item[web]}</td>
                                                <td className="px-4 py-2 border-r text-blue-600">{item[sap]}</td>
                                                <td colSpan={5} className="px-4 py-2 text-center text-red-400 italic">필드 없음</td>
                                            </tr>
                                    );
                                }
                            } else {
                                return (
                                        <tr key={idx} className="hover:bg-gray-50">
                                            <td className="px-4 py-2 border-r truncate" title={item.webField}>{item.webField}</td>
                                            <td className="px-4 py-2 border-r truncate" title={type === 'export' ? item.sapParam : item.sapField}>{type === 'export' ? item.sapParam : item.sapField}</td>
                                            <td className="px-4 py-2 border-r text-center truncate">{item.type}</td>
                                            <td className="px-4 py-2 border-r text-center">{item.size}</td>
                                            <td className="px-4 py-2 bg-blue-50/30 text-gray-600 truncate">{item.example || '-'}</td>
                                        </tr>
                                );
                            }
                        })}
                        </tbody>
                    </table>
                </div>
        );
    };

    if (loading) return <div>Loading...</div>;

    // --- 뷰 모드 ---
    if (!isEditing) {
        return (
                <div className="bg-white shadow-lg rounded-lg min-h-[80vh] p-8">
                    <div className="flex justify-between items-start mb-8 border-b pb-4">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
                                <span className="bg-blue-600 text-white text-xs px-2 py-1 rounded">IF ID</span>
                                {def.id}
                            </h1>
                            <p className="text-gray-500 mt-1 text-sm">{def.description}</p>
                            <div className="mt-2 text-sm font-mono bg-gray-100 inline-block px-2 py-1 rounded text-gray-700">
                                RFC: <strong>{def.rfcFunction}</strong>
                            </div>
                        </div>
                        <div className="flex gap-2">
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

    // --- 편집 모드 ---
    const { web, sap } = (activeTab === 'table' || activeTab === 'return') ? getKeys(activeTab) : { web: '', sap: '' };

    return (
            <div className="bg-white shadow-lg rounded-lg min-h-[80vh] flex flex-col border-2 border-blue-500">
                <div className="p-4 bg-blue-50 border-b border-blue-200 flex justify-between items-center">
                    <span className="font-bold text-blue-800 text-lg">✏️ 편집 모드</span>
                    <div className="space-x-2">
                        <button onClick={() => id ? setIsEditing(false) : navigate('/')} className="px-3 py-1 bg-white border border-gray-300 rounded text-sm">취소</button>
                        <button onClick={handleSave} className="px-4 py-1 bg-blue-600 text-white rounded text-sm font-bold hover:bg-blue-700 shadow">저장 완료</button>
                    </div>
                </div>

                <div className="p-6 border-b bg-white">
                    <div className="grid grid-cols-2 gap-6 mb-4">
                        <div><label className="block text-sm font-medium text-gray-700">Interface ID (URL)</label><input type="text" name="id" value={def.id} onChange={handleBasicChange} disabled={!!id} className="mt-1 block w-full border rounded p-2 bg-gray-50" /></div>
                        <div><label className="block text-sm font-medium text-gray-700">RFC Function Name</label><input type="text" name="rfcFunction" value={def.rfcFunction} onChange={handleBasicChange} className="mt-1 block w-full border rounded p-2" /></div>
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
                                <div className="grid grid-cols-12 gap-2 text-xs font-bold text-gray-500 uppercase px-2 mb-2">
                                    <div className="col-span-3">Web Field</div>
                                    <div className="col-span-3">SAP Field</div>
                                    <div className="col-span-2">Type</div>
                                    <div className="col-span-1 text-center">Len</div>
                                    <div className="col-span-2 text-blue-600">Example</div>
                                    <div className="col-span-1 text-center">Del</div>
                                </div>
                        )}

                        {(activeTab === 'import' || activeTab === 'export') && (
                                <div className="space-y-2">
                                    {(activeTab === 'import' ? def.importMapping : def.exportMapping)?.map((row: any, idx: number) => (
                                            <div key={idx} className="grid grid-cols-12 gap-2 items-center bg-gray-50 p-2 rounded border hover:bg-gray-100 transition-colors">
                                                <div className="col-span-3"><input type="text" value={row.webField} onChange={(e) => handleMappingChange(activeTab, idx, 'webField', e.target.value)} className="w-full border rounded p-1 text-sm" placeholder="Web"/></div>
                                                <div className="col-span-3"><input type="text" value={activeTab === 'export' ? row.sapParam : row.sapField} onChange={(e) => handleMappingChange(activeTab, idx, activeTab === 'export' ? 'sapParam' : 'sapField', e.target.value)} className="w-full border rounded p-1 text-sm bg-yellow-50" placeholder="SAP"/></div>

                                                <div className="col-span-2">
                                                    <select
                                                            value={row.type || 'STRING'}
                                                            onChange={(e) => handleMappingChange(activeTab, idx, 'type', e.target.value)}
                                                            className="w-full border rounded p-1 text-sm text-center bg-white"
                                                    >
                                                        {SAP_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                                                    </select>
                                                </div>

                                                <div className="col-span-1"><input type="text" value={row.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleMappingChange(activeTab, idx, 'size', val))} className="w-full border rounded p-1 text-sm text-center" placeholder="Len"/></div>
                                                <div className="col-span-2"><input type="text" value={row.example || ''} onChange={(e) => handleMappingChange(activeTab, idx, 'example', e.target.value)} className="w-full border rounded p-1 text-sm bg-blue-50" placeholder="예시"/></div>
                                                <div className="col-span-1 text-center"><button onClick={() => removeRow(activeTab, idx)} className="text-red-500 font-bold hover:bg-red-100 rounded w-6 h-6 flex items-center justify-center mx-auto">×</button></div>
                                            </div>
                                    ))}
                                </div>
                        )}

                        {(activeTab === 'table' || activeTab === 'return') && (
                                <div className="space-y-6">
                                    {getTableList(activeTab)?.map((table: any, tIdx: number) => (
                                            <div key={tIdx} className="border-2 border-blue-100 rounded-lg bg-white overflow-hidden shadow-sm">
                                                <div className="bg-blue-50 p-3 border-b flex items-center gap-4">
                                                    <div className="flex-1"><input type="text" value={table[web]} onChange={(e) => handleTablePropChange(activeTab, tIdx, web, e.target.value)} className="w-full border rounded px-2 py-1 text-sm font-bold text-blue-800" placeholder="Web List Key"/></div>
                                                    <span className="text-gray-400">↔</span>
                                                    <div className="flex-1"><input type="text" value={table[sap]} onChange={(e) => handleTablePropChange(activeTab, tIdx, sap, e.target.value)} className="w-full border rounded px-2 py-1 text-sm font-bold text-blue-800 bg-yellow-50" placeholder="SAP Table"/></div>
                                                    <button onClick={() => removeRow(activeTab, tIdx)} className="bg-red-100 text-red-600 px-3 py-1 rounded text-xs font-bold ml-2">테이블 삭제</button>
                                                </div>

                                                <div className="p-3 bg-gray-50">
                                                    <div className="flex justify-between mb-2"><span className="text-xs font-semibold text-gray-500">Fields Mapping</span><button onClick={() => addTableField(activeTab, tIdx)} className="text-blue-600 text-xs border bg-white px-2 py-1 rounded">+ 필드 추가</button></div>
                                                    <div className="grid grid-cols-12 gap-2 text-[10px] text-gray-500 uppercase px-1 mb-1">
                                                        <div className="col-span-3">Web</div><div className="col-span-3">SAP</div><div className="col-span-2">Type</div><div className="col-span-1 text-center">Len</div><div className="col-span-2 text-blue-600">Ex</div><div className="col-span-1"></div>
                                                    </div>

                                                    <div className="space-y-1">
                                                        {table.fields?.map((field: any, fIdx: number) => (
                                                                <div key={fIdx} className="grid grid-cols-12 gap-2 items-center">
                                                                    <div className="col-span-3"><input type="text" value={field.webField} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'webField', e.target.value)} className="w-full border rounded p-1 text-sm"/></div>
                                                                    <div className="col-span-3"><input type="text" value={field.sapField} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'sapField', e.target.value)} className="w-full border rounded p-1 text-sm bg-yellow-50"/></div>

                                                                    {/* [수정] Select Box로 변경 */}
                                                                    <div className="col-span-2">
                                                                        <select
                                                                                value={field.type || 'STRING'}
                                                                                onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'type', e.target.value)}
                                                                                className="w-full border rounded p-1 text-sm text-center bg-white"
                                                                        >
                                                                            {SAP_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                                                                        </select>
                                                                    </div>

                                                                    <div className="col-span-1"><input type="text" value={field.size || ''} onChange={(e) => handleNumberInput(e, (val) => handleTableFieldChange(activeTab, tIdx, fIdx, 'size', val))} className="w-full border rounded p-1 text-xs text-center"/></div>
                                                                    <div className="col-span-2"><input type="text" value={field.example || ''} onChange={(e) => handleTableFieldChange(activeTab, tIdx, fIdx, 'example', e.target.value)} className="w-full border rounded p-1 text-xs bg-blue-50"/></div>
                                                                    <div className="col-span-1 text-center"><button onClick={() => removeTableField(activeTab, tIdx, fIdx)} className="text-gray-400 hover:text-red-500 font-bold">×</button></div>
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
            </div>
    );
};

export default InterfaceEditor;