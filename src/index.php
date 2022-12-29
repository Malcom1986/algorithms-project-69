<?php

declare(strict_types=1);

namespace App;

use Illuminate\Support\Str;
use Illuminate\Support\Collection;

class SearchEngine
{
    private Collection $index;

    public function __construct(array $docs)
    {
        $this->index = $this->buildIndex($docs);
    }

    public function search(string $text): array
    {
        $textTerms = Str::of($text)
            ->explode(' ')
            ->map('normalize')
            ->filter();

        $currentIndex = $this->index->only($textTerms);

        $groupByDocId = $currentIndex
            ->values()
            ->flatten(1)
            ->groupBy('docId')
            ;

        $currentDocsIds = $groupByDocId->keys();

        $weightedDocs = $currentDocsIds
            ->reduce(function (Collection $acc, string $docId) use ($groupByDocId): Collection {
                /** @var Collection $values */
                $values = $groupByDocId->get($docId);
                $sumIdf = $values->sum('tfIDF');

                return $acc->put($docId, $sumIdf);
            }, collect([]));

        return $currentDocsIds->sort(function ($docId2, $docId1) use ($weightedDocs) {
            return $weightedDocs->get($docId1) <=> $weightedDocs->get($docId2);
        })->values()->toArray();
    }

    private function buildIndex(array $docs): Collection
    {
        $docsCount = count($docs);

        $index = collect($docs)
        ->keyBy('id')
        ->map(function (array $doc, string $docId): Collection {
            $terms = Str::of($doc['text'])
            ->explode(' ')
            ->map(fn($token) => normalize($token))
            ->filter();

            return $terms->countBy()
                ->map(function (int $count) use ($docId, $terms): array {
                    return [
                        'docId' => $docId,
                        'termFrequency' => $count / $terms->count(),
                        'count' => $count,
                    ];
                });

            return $terms;
        })
        ->reduce(function (Collection $acc, Collection $docTerms): Collection {
            $keys = $docTerms->keys();

            $keys->each(function ($term) use ($acc, $docTerms) {
                if ($acc->has($term)) {
                    /** @var Collection $termDocs */
                    $termDocs = $acc[$term];
                    $acc[$term] = [...$termDocs, $docTerms[$term]];
                } else {
                    $acc[$term] = [$docTerms[$term]];
                }
            });

            return $acc;
        }, collect([]))
        ->map(function (array $termDocs) use ($docsCount) {
            $termDocsCount = count($termDocs);
            return collect($termDocs)
                ->map(function (array $termDoc) use ($docsCount, $termDocsCount) {
                    $docIdf = calcIDF($docsCount, $termDocsCount);
                    $tfIDF = $termDoc['termFrequency'] * $docIdf;
                    return [
                        ...$termDoc,
                        'tfIDF' => $tfIDF,
                    ];
                });
        });

        return $index;
    }
}

function search(array $docs, string $text): array
{
    $engine = new SearchEngine($docs);
    return $engine -> search($text);
}

function normalize(string $token): ?string
{
    preg_match_all('/\w+/', $token, $matches);
    $result = collect($matches)->flatten()->join('');

    return is_null($result) ? null : Str::lower($result);
}

function calcIDF(int $docsCount, int $termDocsCount): float
{
    $num = 1 + ($docsCount - $termDocsCount + 1) / ($termDocsCount + 0.50);

    return log($num, 2);
}
