#!/usr/bin/env python3
"""Publish a pinned alignment manifest to Arxana's Futon API and verify readback.

No source links are guessed: all source hashes must resolve before any write.
Entity source uses Arxana's JSON-in-source convention; hyperedges have role-labelled
ends. The service URL and penholder are explicit arguments, never environment guesses.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import urllib.parse
import urllib.request


def validate_sources(manifest):
    for entity in manifest['entities']:
        source = json.loads(entity['source'])
        revision = source.get('revision')
        if revision:
            raw = subprocess.check_output(['git', 'show', f"{revision}:{source['path']}"], cwd=source['repo'])
        else:
            raw = (Path(source['repo']) / source['path']).read_bytes()
        if hashlib.sha256(raw).hexdigest() != source['sha256']:
            raise ValueError(f"source pin mismatch: {entity['id']}")
        text = raw.decode('utf-8')
        finder = source['finder']
        for key in ('text', 'symbol'):
            if key in finder and finder[key] not in text:
                raise ValueError(f"finder no longer resolves: {entity['id']}")
    ids = {e['id'] for e in manifest['entities']}
    for edge in manifest['hyperedges']:
        if not all(end['entity-id'] in ids and end['role'] for end in edge['hx/endpoints']):
            raise ValueError('unresolved hyperedge endpoint')


def publish(manifest, base, penholder, receipt):
    validate_sources(manifest)
    records = []

    def request(path, payload=None):
        data = None if payload is None else json.dumps(dict(payload, penholder=penholder)).encode()
        req = urllib.request.Request(base.rstrip('/') + path, data=data,
                                     headers={'Content-Type': 'application/json',
                                              'Accept': 'application/json', 'X-Penholder': penholder})
        with urllib.request.urlopen(req, timeout=30) as response:
            result = json.load(response)
            records.append({'method': req.get_method(), 'path': path,
                            'http-status': response.status, 'response': result})
            return result

    try:
        for entity in manifest['entities']:
            request('/entity', entity)
            stored = request('/entity/' + urllib.parse.quote(entity['id'], safe=''))['entity']
            if stored['source'] != entity['source']:
                raise ValueError('entity readback differs')
        for edge in manifest['hyperedges']:
            result = request('/hyperedge', edge)
            if result.get('ok') is not True:
                raise ValueError(f'hyperedge write refused: {result}')
            stored = request('/hyperedge/' + urllib.parse.quote(edge['hx/id'], safe=''))
            expected = {(e['role'], e['entity-id']) for e in edge['hx/endpoints']}
            actual = {(e['role'], e['entity-id']) for e in stored['hx/ends']}
            if stored['hx/id'] != edge['hx/id'] or expected != actual or stored['hx/props'] != edge['hx/props']:
                raise ValueError('hyperedge roles, endpoints or claims differ on readback')
        return {'status': 'published-and-verified', 'entities': len(manifest['entities']),
                'hyperedges': [e['hx/id'] for e in manifest['hyperedges']]}
    finally:
        Path(receipt).write_text(json.dumps({'base': base, 'requests': records}, indent=2) + '\n')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('manifest')
    parser.add_argument('--base', required=True)
    parser.add_argument('--penholder', required=True)
    parser.add_argument('--receipt', required=True)
    args = parser.parse_args()
    print(json.dumps(publish(json.loads(Path(args.manifest).read_text()), args.base,
                             args.penholder, args.receipt)))
