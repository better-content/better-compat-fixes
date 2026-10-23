#!/usr/bin/env python3
"""After-JVM diagnostic completeness only. Never accepts a product/provider or waives soil failures."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re

SCHEMA = 'bc.native.rooty-soil-witness.v1'
WARNING = re.compile(r'Rooty\s+Dirt\s+block\s+NOT\s+FOUND\s+for\s+soil\s+([a-z0-9_.-]+:[a-z0-9_./-]+)')
ID = re.compile(r'[a-z0-9_.-]+:[a-z0-9_./-]+\Z')
SYMBOL = re.compile(r'[A-Za-z0-9_.$<>/-]+\Z')
TOP = {'schema', 'observerComplete', 'fullLogReconciliationRequired', 'callerPrefixLimit', 'recordLimit',
       'byteLimit', 'recordBytes', 'calls', 'finished', 'open', 'unsupported', 'dropped', 'overflow',
       'missing', 'doubleForward', 'late', 'exceptions', 'records'}
ROW = {'species', 'soil', 'root', 'fertility', 'soilStateId', 'generation', 'thread', 'callerPrefix',
       'warnings', 'setCalls', 'setBlock', 'returned', 'exception'}


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def integer(value):
    return type(value) is int and -(1 << 31) <= value < (1 << 31)


def position(value):
    return type(value) is list and len(value) == 3 and all(integer(n) for n in value)


def identifier(value):
    return isinstance(value, str) and len(value) <= 128 and ID.fullmatch(value)


def verify(raw, debug, latest):
    require(len(raw.encode('utf-8')) <= 131072, 'maximum artifact bytes')
    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, 'duplicate JSON key')
            result[key] = value
        return result
    report = json.loads(raw, object_pairs_hook=unique_object)
    require(type(report) is dict and set(report) == TOP, 'schema fields')
    require(report['schema'] == SCHEMA and report['observerComplete'] is True
            and report['fullLogReconciliationRequired'] is True, 'observer incomplete/unknown schema')
    require(report['callerPrefixLimit'] == 8 and type(report['recordLimit']) is int
            and 1 <= report['recordLimit'] <= 128 and type(report['byteLimit']) is int
            and 2048 <= report['byteLimit'] <= 131072, 'bounds')
    require(len(raw.encode('utf-8')) <= report['byteLimit'], 'whole-artifact byte overflow')
    for counter in ('recordBytes', 'calls', 'finished', 'open', 'unsupported', 'dropped', 'overflow',
                    'missing', 'doubleForward', 'late', 'exceptions'):
        require(type(report[counter]) is int and report[counter] >= 0, 'counter type/range')
    require(all(report[key] == 0 for key in ('open', 'dropped', 'overflow', 'missing', 'doubleForward',
                                           'late', 'exceptions')), 'missing/overflow/late/exception/double-forward')
    require(report['calls'] == report['finished'], 'unfinished scopes')
    rows = report['records']
    require(type(rows) is list and len(rows) <= report['recordLimit']
            and report['unsupported'] == len(rows), 'record completeness')
    for row in rows:
        require(type(row) is dict and set(row) == ROW, 'record fields')
        require(identifier(row['species']) and identifier(row['soil']) and position(row['root'])
                and integer(row['fertility']) and integer(row['soilStateId']), 'public scalar locals')
        gen = row['generation']
        require(type(gen) is dict and set(gen) == {'origin', 'root', 'radius', 'facing', 'secondChance'}
                and position(gen['origin']) and position(gen['root']) and integer(gen['radius'])
                and integer(gen['facing']) and type(gen['secondChance']) is bool, 'missing generation')
        require(row['thread'] in ('server', 'worldgen', 'other'), 'thread category')
        callers = row['callerPrefix']
        require(type(callers) is list and 1 <= len(callers) <= 8, 'missing/bounded caller prefix')
        for caller in callers:
            require(type(caller) is dict and set(caller) == {'class', 'method', 'line'}, 'caller fields')
            require(all(isinstance(caller[key], str) and 0 < len(caller[key]) <= 128
                        and SYMBOL.fullmatch(caller[key]) for key in ('class', 'method'))
                    and integer(caller['line']), 'caller public symbol')
        state = row['setBlock']
        require(type(state) is dict and set(state) == {'pos', 'block', 'stateId', 'flags', 'returned'}
                and position(state['pos']) and identifier(state['block']) and integer(state['stateId'])
                and integer(state['flags']) and type(state['returned']) is bool, 'actual setBlock args/result')
        require(type(row['warnings']) is int and row['warnings'] == 1 and type(row['setCalls']) is int
                and row['setCalls'] == 1 and row['returned'] is False and row['exception'] is None,
                'native return/forward/exception')
    require(report['recordBytes'] == sum(len(json.dumps(row, separators=(',', ':'))) + 1 for row in rows),
            'record byte accounting')
    actual = Counter(row['soil'] for row in rows)
    debug_hits, latest_hits = Counter(WARNING.findall(debug)), Counter(WARNING.findall(latest))
    require(debug_hits == latest_hits == actual, 'complete DEBUG/latest WARN soil-count reconciliation')
    return {'schema': 'bc.native.rooty-soil-witness-review.v1', 'diagnostic_complete': True,
            'product_qualification': False, 'soil_operations_still_reject_product': bool(rows),
            'warning_operations': len(rows), 'soil_counts': dict(actual),
            'observer_scope': 'bounded caller prefix, not complete stack; public runtime blockstate IDs'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--witness', type=Path, required=True)
    parser.add_argument('--debug', type=Path, required=True)
    parser.add_argument('--latest', type=Path, required=True)
    args = parser.parse_args()
    files = [args.witness, args.debug, args.latest]
    require(all(path.is_file() and not path.is_symlink() for path in files), 'missing/symlink input')
    result = verify(*(path.read_text(encoding='utf-8') for path in files))
    result['inputs'] = [{'path': str(path.resolve()), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
                        for path in files]
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
