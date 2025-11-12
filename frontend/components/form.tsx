"use client"

import { useState } from "react"
import { Button } from "./ui/button"
import { Field, FieldLabel, FieldDescription, FieldError } from "./ui/field"
import { Input } from "./ui/input"
import { useActionState } from "react"
import { shortenAction } from "@/app/actions"
import { Spinner } from "./ui/spinner"

export default function Form() {
    const [state, formAction, pending] = useActionState(shortenAction, undefined)
    const [hasAlias, setHasAlias] = useState(false)

    return (
        <form
            action={formAction}
            className="space-y-6 max-w-md mx-auto p-6 rounded-2xl border bg-background"
        >
            <Field>
                <FieldLabel>URL to shorten</FieldLabel>
                <Input
                    placeholder="https://example.com"
                    name="fullUrl"
                    required
                />
                <FieldDescription>
                    Must start with <code>https://</code> or <code>http://</code>
                </FieldDescription>
                <FieldError>{state?.errors?.properties?.fullUrl?.errors.map((error, index) => (
                    <div key={index}>{error}</div>
                ))}</FieldError>
            </Field>

            {(hasAlias || !!state?.errors?.properties?.alias) ? (
                <Field>
                    <FieldLabel>Custom alias</FieldLabel>
                    <Input
                        placeholder="e.g. mylink123"
                        name="alias"
                    />
                    <FieldDescription>
                        3-32 letters or numbers
                    </FieldDescription>

                    <Button
                        type="button"
                        variant="ghost"
                        onClick={() => setHasAlias(false)}
                        className="text-sm mt-1"
                    >
                        Remove alias
                    </Button>
                    <FieldError>{state?.errors?.properties?.alias?.errors.map((error, index) => (
                        <div key={index}>{error}</div>
                    ))}</FieldError>
                </Field>
            ) : (
                <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setHasAlias(true)}
                >
                    + Add alias
                </Button>
            )}

            <Button type="submit" className="w-full" disabled={pending}>
                {pending ? <Spinner /> : "Shorten URL"}
            </Button>

            {state?.shortUrl && (
                <p className="text-sm mt-4">
                    Short URL:{" "}
                    <a href={state.shortUrl} className="underline text-blue-600">
                        {state.shortUrl}
                    </a>
                </p>
            )}
        </form>
    )
}