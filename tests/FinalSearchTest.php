<?php

namespace Tests;

use PHPUnit\Framework\TestCase;

use function search;

final class FinalSearchTest extends TestCase
{
    public function testSearch(): void
    {
        $searchText = 'trash island';
        $docs = collect([
        ['id' => 'garbage_patch_NG'],
        ['id' => 'garbage_patch_ocean_clean'],
        ['id' => 'garbage_patch_wiki'],
        ])
        ->map(function ($doc) {
            return [
            ...$doc,
            'text' => $this->readFixture($doc['id']),
            ];
        });
        $actual = search($docs->toArray(), $searchText);
        $expected = $docs->pluck('id')->toArray();

        $this->assertSame($expected, $actual);
    }

    public function testSearchWithSpam(): void
    {
        $searchText = 'the trash island is a';
        $docs = collect([
            ['id' => 'garbage_patch_NG'],
            ['id' => 'garbage_patch_ocean_clean'],
            ['id' => 'garbage_patch_wiki'],
            ['id' => 'garbage_patch_spam'],
        ])->map(fn ($doc) => [...$doc, 'text' => $this->readFixture($doc['id'])]);
        $actual = search($docs->toArray(), $searchText);
        $expected = $docs->pluck('id')->toArray();

        $this->assertSame($expected, $actual);
    }

    public function testSearchEmpty(): void
    {
        $searchText = '';
        $docs = [];
        $expected = [];
        $actual = search($docs, $searchText);

        $this->assertSame($expected, $expected);
    }


    public function testSearchShortStrings(): void
    {
        $searchText = 'shoot at me, nerd';
        $doc1 = "I can't shoot straight unless I've had a pint!";
        $doc2 = "Don't shoot shoot shoot that thing at me.";
        $doc3 = "I'm your shooter.";
        $docs = [
            ['id' => 'doc1', 'text' => $doc1],
            ['id' => 'doc2', 'text' => $doc2],
            ['id' => 'doc3', 'text' => $doc3],
        ];

        $actual = search($docs, $searchText);
        $expected = ['doc2', 'doc1'];

        $this->assertSame($expected, $expected);
    }

    private function readFixture($name): string
    {
        return file_get_contents(implode('/', [__DIR__, 'fixtures', $name]));
    }
}
